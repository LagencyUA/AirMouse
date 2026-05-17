using AirMouse_Host.models;
using AirMouse_Host.services;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;

namespace AirMouse_Host
{
    public class TcpServer
    {
        private TcpListener server;
        private readonly AuthManager auth;
        public Session CurrentSession { get; private set; }
        public event Action<string> OnStatusChanged;

        private readonly InputService inputService = new();

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
            var stream = client.GetStream();
            using var reader = new StreamReader(stream, Encoding.UTF8);

            string json = reader.ReadLine();

            if (string.IsNullOrEmpty(json))
                return;

            using JsonDocument doc = JsonDocument.Parse(json);
            string type = doc.RootElement.GetProperty("Type").GetString();

            if (type == "auth")
                HandleAuth(client, json);

            if (CurrentSession == null)
            {
                System.Diagnostics.Debug.WriteLine("No active session, ignoring commands");
                return;
            }

            while (CurrentSession != null)
            {
                if (client == null || !client.Connected)
                    break;

                try
                {
                    json = reader.ReadLine();

                    if (json == null)
                        break;

                    Execute(json);
                }
                catch (Exception ex) when (ex is ObjectDisposedException || ex is IOException)
                {
                    // Socket was closed, exit loop to clean up session
                    break;
                }
            }
            Disconnect();
        }

        private void HandleAuth(TcpClient client, string json)
        {
            var packet = JsonSerializer.Deserialize<AuthPacket>(json);
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

        private void HandleSystem(TcpClient client, string json)
        {
            var packet = JsonSerializer.Deserialize<SystemPacket>(json);

            if (packet?.Action == "disconnect")
            {
                System.Diagnostics.Debug.WriteLine("Client disconnected");

                Disconnect();
            }
        }

        private void Execute(string json)
        {
            System.Diagnostics.Debug.WriteLine($"RAW: {json}"); //debug
            
            var packet = JsonSerializer.Deserialize<InputPacket>(json);

            switch (packet?.Type)
            {
                case "input":
                    inputService.Process(packet);
                    break;
                case "system":
                    HandleSystem(CurrentSession.Client, json);
                    return;
            }
        }

        private void Disconnect()
        {
            try
            {
                CurrentSession?.Client?.Close();
            }
            catch { }

            CurrentSession = null;
            OnStatusChanged?.Invoke("Waiting for device to connect");
        }

        public void DisconnectFromHost(string message = "Server closed connection")
        {
            if (CurrentSession == null)
                return;

            try
            {
                var packet = new SystemPacket
                {
                    Type = "system",
                    Action = "disconnect",
                    Message = message
                };

                string json = JsonSerializer.Serialize(packet) + "\n";
                byte[] bytes = Encoding.UTF8.GetBytes(json);
                var stream = CurrentSession.Client.GetStream();

                stream.Write(bytes, 0, bytes.Length);
            }
            catch { }

            Disconnect();
        }
    }
}
