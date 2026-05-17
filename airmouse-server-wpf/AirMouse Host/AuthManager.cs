using System;
using System.Linq;

namespace AirMouse_Host
{
    public class AuthManager
    {
        private readonly int _codeLength;
        private const string Characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static readonly Random _rnd = new Random();

        public string CurrentPin { get; private set; }

        // Довжина передається при створенні об'єкта, за замовчуванням 6
        public AuthManager(int codeLength = 6)
        {
            _codeLength = codeLength;
            GeneratePin();
        }

        public void GeneratePin()
        {
            // Генеруємо рядок випадкових символів заданої довжини
            char[] result = new char[_codeLength];
            for (int i = 0; i < _codeLength; i++)
            {
                result[i] = Characters[_rnd.Next(Characters.Length)];
            }

            CurrentPin = new string(result);
        }

        public bool Validate(string pin)
        {
            return string.Equals(pin, CurrentPin, StringComparison.Ordinal);
        }
    }
}