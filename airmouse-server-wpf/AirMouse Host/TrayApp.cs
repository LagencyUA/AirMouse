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
        private ToolStripLabel currLocalIP;
        private ToolStripLabel currLocalPort;
        private ToolStripMenuItem changePortButton;

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
            server = new TcpServer(Settings, auth);
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
            // App name and version
            var appName = new ToolStripLabel("AirMouse v0.2.0");
            menu.Items.Add(appName);
            menu.Items.Add(new ToolStripSeparator());

            // Local IP and Port
            currLocalIP = new ToolStripLabel($"Local IP: {GetLocalIpAddress()}");
            currLocalPort = new ToolStripLabel($"Port: {Settings.Port}");
            menu.Items.Add(currLocalIP);
            menu.Items.Add(currLocalPort);
            menu.Items.Add(new ToolStripSeparator());

            // Session PIN and connection status
            pinLabel = new ToolStripLabel($"Session PIN: {auth.CurrentPin}");
            menu.Items.Add(pinLabel);

            currDevLabel = new ToolStripLabel("No device is connected");
            menu.Items.Add(currDevLabel);

            disconnectClientButton = new ToolStripMenuItem("Disconnect Device", null, KickClient_Click);
            disconnectClientButton.Enabled = false;
            menu.Items.Add(disconnectClientButton);

            menu.Items.Add(new ToolStripSeparator());

            // Settings submenu and Exit
            settingsSubMenu = new ToolStripMenuItem("Settings");

            togglePinButton = new ToolStripMenuItem();
            togglePinButton.Click += TogglePinMode_Click;

            changePortButton = new ToolStripMenuItem("Change Port", null, changePort);

            settingsSubMenu.DropDownItems.Add(togglePinButton);
            settingsSubMenu.DropDownItems.Add(changePortButton);
            UpdatePinButtonText();

            menu.Items.Add(settingsSubMenu);
            menu.Items.Add("Exit", null, Exit);

            tray.ContextMenuStrip = menu;
        }

        private void UpdateStatus(string status)
        {
            if (menu.InvokeRequired)
            {
                menu.Invoke(new Action(() => UpdateStatus(status)));
                return;
            }

            currDevLabel.Text = status;
            bool isConnected = server.CurrentSession != null;

            disconnectClientButton.Enabled = isConnected;

            if (!isConnected)
            {
                auth.RefreshPin();
                pinLabel.Text = $"Session PIN: {auth.CurrentPin}";
            }
            currLocalIP.Text = $"Local IP: {GetLocalIpAddress()}";

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
            if (auth.Settings.UseStaticPin)
            {
                togglePinButton.Text = "Switch to Dynamic PIN";
            }
            else
            {
                togglePinButton.Text = "Set Static PIN...";
            }

            pinLabel.Text = $"Session PIN: {auth.CurrentPin}";
        }
        private string PromptInput(string title, string promptText, string defaultValue)
        {
            string input = Interaction.InputBox(promptText, title, defaultValue);
            return string.IsNullOrEmpty(input) ? null : input;
        }
        private void KickClient_Click(object sender, EventArgs e)
        {
            server.DisconnectFromHost("Forcefully disconnected by host");
        }

        public void Exit(object sender, EventArgs e)
        {
            tray.Visible = false;
            server.DisconnectFromHost(); // Ensure we close all connections and stop the server
            Application.Exit();
        }

        public string GetLocalIpAddress()
        {
            try
            {
                using (Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0))
                {
                    socket.Connect("8.8.8.8", 65530);

                    if (socket.LocalEndPoint is IPEndPoint endPoint)
                    {
                        return endPoint.Address.ToString();
                    }
                }
            }
            catch
            {
                return "127.0.0.1";
            }

            return "127.0.0.1";
        }

        public void changePort(object sender, EventArgs e)
        {
            string input = PromptInput("Change Port", "Enter new port number (1-65535):", Settings.Port.ToString());
            if (int.TryParse(input, out int newPort) && newPort >= 1 && newPort <= 65535)
            {
                server?.Stop();

                Settings.Port = newPort;
                SettingsManager.Save(Settings);
                
                LaunchTcpServer();

                currLocalPort.Text = $"Port: {Settings.Port}";
                currLocalIP.Text = $"Local IP: {GetLocalIpAddress()}";
            }
            else
            {
                MessageBox.Show("Invalid port number. Please enter a value between 1 and 65535.");
            }

        }
    }
}
