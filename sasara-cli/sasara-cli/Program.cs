using CeVIO.Talk.RemoteService;

namespace sasara_cli
{
    class Program
    {
        static void Main(string[] args)
        {
            ServiceControl.StartHost(false);

            Talker talker = new Talker();

            // キャスト設定
            talker.Cast = "さとうささら";

            // （例）音量設定
            talker.Volume = 100;

            // 再生する文字列を取得
            if (args.Length == 0 || args[0].Length == 0)
            {
                return;
            }
            string text = args[0];

            // 音声ファイルを保存
            if (!talker.OutputWaveToFile(text, "C:/WINDOWS/TEMP/sasara_output.wav")) {
                throw new System.InvalidOperationException();
            }
        }
    }
}
