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
        private readonly object sessionLock = new();
        public Session CurrentSession { get; private set; }
        public event Action<string> OnStatusChanged;

        private readonly InputService inputService = new();
        private readonly AppSettings Settings;

        private bool isStopping = false;

        public TcpServer(AppSettings settings, AuthManager authManager)
        {
            auth = authManager;
            Settings = settings;
        }

        public async Task StartAsync()
        {
            isStopping = false;
            server = new TcpListener(IPAddress.Any, Settings.Port);
            server.Start();
            OnStatusChanged?.Invoke("Waiting for device to connect");

            try
            {
                while (!isStopping)
                {
                    TcpClient client = await server.AcceptTcpClientAsync();

                    _ = Task.Run(() => HandleClient(client));
                }
            }
            catch (Exception) {
                if (isStopping)
                    return;
                else throw;
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
            {
                HandleAuth(client, json);

                // If this client did not become the active session, close and exit.
                if (CurrentSession == null || CurrentSession.Client != client)
                {
                    try { client.Close(); } catch { }
                    System.Diagnostics.Debug.WriteLine("Client not accepted (another session active) - closing");
                    return;
                }
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

            // Only disconnect global session if this client is the active session
            if (CurrentSession != null && CurrentSession.Client == client)
            {
                Disconnect();
            }
            else
            {
                try { client.Close(); } catch { }
            }
        }

        private void HandleAuth(TcpClient client, string json)
        {
            var packet = JsonSerializer.Deserialize<AuthPacket>(json);
            var stream = client.GetStream();

            // If there is already an active session, refuse additional connections
            lock (sessionLock)
            {
                if (CurrentSession != null)
                {
                    var busyResponse = new AuthResponse
                    {
                        Success = false,
                        Message = "another device is already connected"
                    };

                    string busyJson = JsonSerializer.Serialize(busyResponse) + "\n";
                    byte[] busyBytes = Encoding.UTF8.GetBytes(busyJson);
                    try { stream.Write(busyBytes, 0, busyBytes.Length); } catch { }
                    try { client.Close(); } catch { }
                    return;
                }

                bool valid = auth.Validate(packet.Pin);
                var response = new AuthResponse
                {
                    Success = valid,
                    Message = valid ? "connected" : "invalid pin"
                };

                string responseJson = JsonSerializer.Serialize(response) + "\n"; // For line by line
                byte[] bytes = Encoding.UTF8.GetBytes(responseJson);
                try { stream.Write(bytes, 0, bytes.Length); } catch { }

                if (!valid)
                {
                    try { client.Close(); } catch { }
                    return;
                }

                CurrentSession = new Session
                {
                    Client = client,
                    DeviceName = packet.DeviceName,
                    ConnectedAt = DateTime.Now
                };
            }

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

        public void Stop()
        {
            isStopping = true;
            DisconnectFromHost("Server is shutting down");
            try
            {
                server?.Stop();
            }
            catch { }
        }
    }
}
