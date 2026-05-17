using AirMouse_Host.models;
using Microsoft.VisualBasic;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace AirMouse_Host
{
    public class TrayApp : ApplicationContext
    {
        NotifyIcon tray;
        private TcpServer server;
        private AuthManager auth;
        private ContextMenuStrip menu;
        private AppSettings Settings;

        // Dynamic elements
        private ToolStripLabel currDevLabel;
        private ToolStripLabel pinLabel;
        private ToolStripMenuItem settingsSubMenu;
        private ToolStripMenuItem togglePinButton;
        private ToolStripMenuItem disconnectClientButton;

        public TrayApp()
        {
            // Create basic Tray app
            tray = new NotifyIcon();
            tray.Icon = SystemIcons.Application; // Change later on
            tray.Visible = true;
            tray.Text = "AirMouse Host";
            Settings = SettingsManager.Load();
            auth = new AuthManager(Settings);

            BuildSettingsMenu();
            LaunchTcpServer();

        }
        private void LaunchTcpServer()
        {
            server = new TcpServer(auth);
            server.OnStatusChanged += status =>
            {
                UpdateStatus(status);
            };

            _ = Task.Run(async () =>
            {
                try
                {
                    await server.StartAsync();
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Server down: {ex.Message}");
                }
            });

        }
        private void BuildSettingsMenu()
        {
            menu = new ContextMenuStrip();

            var appName = new ToolStripLabel("AirMouse v1");
            menu.Items.Add(appName);

            menu.Items.Add(new ToolStripSeparator());

            pinLabel = new ToolStripLabel($"Session PIN: {auth.CurrentPin}");
            menu.Items.Add(pinLabel);

            currDevLabel = new ToolStripLabel("No device is connected");
            menu.Items.Add(currDevLabel);

            disconnectClientButton = new ToolStripMenuItem("Disconnect Device", null, KickClient_Click);
            disconnectClientButton.Enabled = false; // Вимкнена за замовчуванням
            menu.Items.Add(disconnectClientButton);

            menu.Items.Add(new ToolStripSeparator());

            settingsSubMenu = new ToolStripMenuItem("Settings");

            togglePinButton = new ToolStripMenuItem();
            togglePinButton.Click += TogglePinMode_Click;

            settingsSubMenu.DropDownItems.Add(togglePinButton);
            UpdatePinButtonText();

            menu.Items.Add(settingsSubMenu);
            menu.Items.Add("Exit", null, Exit);

            tray.ContextMenuStrip = menu;
        }

        private void UpdateStatus(string status)
        {
            // Перевіряємо, чи потрібно робити Invoke (якщо ми в іншому потоці)
            // ContextMenuStrip сам не має Invoke, тому використовуємо прихований контроль меню
            if (menu.InvokeRequired)
            {
                menu.Invoke(new Action(() => UpdateStatus(status)));
                return;
            }

            currDevLabel.Text = status;
            // Перевіряємо, чи є активна сесія
            bool isConnected = server.CurrentSession != null;

            // Кнопка від'єднання стає активною тільки якщо хтось підключений
            disconnectClientButton.Enabled = isConnected;

            // Якщо пристрій відключився, оновлюємо динамічний PIN (якщо активований динамічний режим)
            if (!isConnected)
            {
                auth.RefreshPin();
                pinLabel.Text = $"Session PIN: {auth.CurrentPin}";
            }
        }

        private void TogglePinMode_Click(object sender, EventArgs e)
        {
            if (!auth.Settings.UseStaticPin)
            {
                string input = PromptInput("Enter Static PIN", "Please enter your permanent PIN code:", auth.Settings.StaticPin);

                if (!string.IsNullOrWhiteSpace(input))
                {
                    auth.ToggleStaticPin(true, input.Trim());
                }
            }
            else
            {
                auth.ToggleStaticPin(false);
            }

            UpdatePinButtonText();
        }
        private void UpdatePinButtonText()
        {
            // Змінюємо текст кнопки в залежності від типу PIN-коду
            if (auth.Settings.UseStaticPin)
            {
                togglePinButton.Text = "Switch to Dynamic PIN";
            }
            else
            {
                togglePinButton.Text = "Set Static PIN...";
            }

            // Оновлюємо відображення PIN-коду на головному екрані меню
            pinLabel.Text = $"Session PIN: {auth.CurrentPin}";
        }
        private string PromptInput(string title, string promptText, string defaultValue)
        {
            string input = Interaction.InputBox(promptText, title, defaultValue);

            // Якщо користувач натиснув Cancel або закрив вікно, повернеться порожній рядок ""
            return string.IsNullOrEmpty(input) ? null : input;
        }
        private void KickClient_Click(object sender, EventArgs e)
        {
            server.DisconnectFromHost("Forcefully disconnected by host");
        }

        private void Exit(object sender, EventArgs e)
        {
            tray.Visible = false;
            server.DisconnectFromHost(); // Ensure we close all connections and stop the server
            Application.Exit();
        }


    }
}
