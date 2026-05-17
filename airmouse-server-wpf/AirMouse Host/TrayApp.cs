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

        private ToolStripLabel currDevLabel;

        public TrayApp()
        {
            // Create basic Tray app
            tray = new NotifyIcon();
            tray.Icon = SystemIcons.Application; // Change later on
            tray.Visible = true;
            tray.Text = "AirMouse Host";

            // Set up auth
            auth = new AuthManager();

            // Build context menu
            menu = new ContextMenuStrip();
            var appName = new ToolStripLabel("AirMouse v1");
            menu.Items.Add(appName);
            menu.Items.Add(new ToolStripSeparator());

            var pinLabel = new ToolStripLabel($"Session PIN: {auth.CurrentPin}");
            menu.Items.Add(pinLabel);
            currDevLabel = new ToolStripLabel("No device is connected");
            menu.Items.Add(currDevLabel);

            menu.Items.Add(new ToolStripSeparator());

            menu.Items.Add("Exit", null, Exit);

            tray.ContextMenuStrip = menu;
            
            
            // Launch tcp server
            server = new TcpServer(auth);
            server.OnStatusChanged += status =>
            {
                // Оскільки подія приходить з Task.Run (фоновий потік),
                // використовуємо метод Invoke для переходу в UI-потік
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
        }

        void Exit(object sender, EventArgs e)
        {
            tray.Visible = false;
            Application.Exit();
        }


    }
}
