using AirMouse_Host.models;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.IO;
using System.Text.Json;

namespace AirMouse_Host
{
    public class TcpServer
    {
        private TcpListener server;
        private readonly AuthManager auth;
        public Session CurrentSession { get; private set; }
        public event Action<string> OnStatusChanged;

        public TcpServer(AuthManager authManager)
        {
            auth = authManager;
            server = new TcpListener(IPAddress.Any, 5000);
        }

        public async Task StartAsync()
        {
            server.Start();
            OnStatusChanged?.Invoke("Waiting for device to connect");

            while (true)
            {
                TcpClient client = await server.AcceptTcpClientAsync();

                _ = Task.Run(() => HandleClient(client));
            }
        }

        private void HandleClient(TcpClient client)
        {
            try
            {
                var stream = client.GetStream();
                using var reader = new StreamReader(stream, Encoding.UTF8);

                string json = reader.ReadLine();

                if (string.IsNullOrEmpty(json))
                    return;

                using JsonDocument doc = JsonDocument.Parse(json);
                string type = doc.RootElement.GetProperty("Type").GetString();

                if (type == "auth")
                {
                    var packet = JsonSerializer.Deserialize<AuthPacket>(json);
                    System.Diagnostics.Debug.WriteLine($"RECEIVED: {json}");
                    HandleAuth(client, packet);
                }

                if (CurrentSession == null)
                {
                    System.Diagnostics.Debug.WriteLine("No active session, ignoring commands");
                    return;
                }

                while (true)
                {
                    json = reader.ReadLine();

                    if (string.IsNullOrEmpty(json))
                        break;

                    System.Diagnostics.Debug.WriteLine($"RAW: {json}");

                    Command cmd = JsonSerializer.Deserialize<Command>(json);

                    Execute(cmd);
                }
            }
            catch
            {
                Disconnect();
            }
        }

        private void HandleAuth(TcpClient client, AuthPacket packet)
        {
            var stream = client.GetStream();
            bool valid = auth.Validate(packet.Pin);
            var response = new AuthResponse
            {
                Success = valid,
                Message = valid ? "connected" : "invalid pin"
            };

            string responseJson = JsonSerializer.Serialize(response) + "\n"; // For line by line
            byte[] bytes = Encoding.UTF8.GetBytes(responseJson);
            stream.Write(bytes, 0, bytes.Length);

            if (!valid)
            {
                client.Close();
                return;
            }

            CurrentSession = new Session
            {
                Client = client,
                DeviceName = packet.DeviceName,
                ConnectedAt = DateTime.Now
            };

            OnStatusChanged?.Invoke(packet.DeviceName);
            System.Diagnostics.Debug.WriteLine("Session started!");
        }

        private void Execute(Command cmd)
        {
            System.Diagnostics.Debug.WriteLine($"Command: {cmd.Type}"); // Debug
            System.Diagnostics.Debug.WriteLine($"Command: {cmd.Button}"); // Debug
        }

        private void Disconnect()
        {
            CurrentSession = null;
            OnStatusChanged?.Invoke("Waiting for device to connect");
        }
    }
}
