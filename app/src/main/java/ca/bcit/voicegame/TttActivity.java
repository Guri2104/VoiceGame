package ca.bcit.voicegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import static ca.bcit.voicegame.TTTutils.*;


public class TttActivity
        extends AppCompatActivity {
    private WebView webView;
//    private AudioChat ac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WebSettings settings;
        System.out.println("started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ttt);
        refreshValues();
        System.out.println("UID value number 1 in TTT is " + UID);
        UID++;
        System.out.println("UID value number 2 in TTT is " + UID);

        webView = (WebView) findViewById(R.id.webView);

        settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        final String htmlString = getString(R.string.TTT_webview);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadData(htmlString, "text/html; charset=utf-8", "UTF8");
    }


    private class WebAppInterface
    {
        private final String TAG = WebAppInterface.class.getName();
        int num_moves = 0;
        int team = -1;
        String sym = "X";
        String opp_sym = "O";
        String move = "X";
        String status = "You are 'X'. Make your move.";
        boolean turn = true;

        @JavascriptInterface
        public void createGame(){
            team = connectToGame(V1, GAME_TTT);
            if (team == -1) return;
            if (team == SYM_O) updateVariablesOnConnection();
//            ac = new AudioChat(UID);
//            ac.startStreamingAudio();
            initializeGame(turn, status);
        }

        @JavascriptInterface
        public void makeMove(final @NonNull String cell){
            if (num_moves == 0 && !turn){
                handleOpponentMove();
            }
            else {
                handleMyMove(cell);
            }
        }

        @JavascriptInterface
        public void handleOpponentMove(){

            int response = readOpponentMove();
            final String pos = Integer.toString(response);
            if (GAME_STATE != RUNNING) handleGameEndChoice(pos);
            else {
                if (response == -1) status = "Error in receiving opponent move.";
                move =  opp_sym;
                status = "Make your move.";
                turn = true;
                num_moves++;
                updateBoardAndStatus(move, pos, status);
            }
        }

        public void handleMyMove(final @NonNull String cell){
            if (!sendMove(Byte.parseByte(cell))) {
                status = "Error sending move, please try again";
                updateStatus(status);
                return;
            }
            move =  sym;
            status = "Wait for opponent's turn.";
            turn = !turn;
            num_moves++;
            updateBoardAndStatus(move, cell, status);
            handleOpponentMove();

        }

        @JavascriptInterface
        public void handleGameEndChoice(final String choice){
            String state = "game end state";
            if (GAME_STATE == WIN) state = "Congratulations, You won!";
            else if (GAME_STATE == LOSS) state = "Sorry, You lost the game :|";
            else if (GAME_STATE == TIE) state = "Close match, You tied :)";
            else if (GAME_STATE == OPP_LEFT) state = "Your opponent left the game D:";

            updateBoardAndStatus(opp_sym, choice, state);
        }

        @JavascriptInterface
        public void handleGameQuitChoice(final int choice){
            if (choice == 0){
                finish();
                startActivity(getIntent());
            }
            else {
                finish();
            }
        }

        @JavascriptInterface
        public void updateVariablesOnConnection(){
            sym = "O";
            opp_sym = "X";
            turn = false;
            status = "You are 'O'. Wait for opponent's turn.";

        }

        @JavascriptInterface
        public void updateBoardAndStatus(final @NonNull String val, final @NonNull String id, final @NonNull String stat){
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("updateBoard('" + val+ "', '" + id + "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            System.out.println("Done");
                        }
                    });
                    webView.evaluateJavascript("updateStatus('" + stat+ "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            System.out.println("Done");
                        }
                    });
                }
            });
        }

        @JavascriptInterface
        public void updateStatus(final @NonNull String stat){
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("updateStatus('" + stat + "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            System.out.println("Done");
                        }
                    });
                }
            });
        }

        @JavascriptInterface
        public void initializeGame(final boolean isTurn, final @NonNull String stat){
            final String turn_string = isTurn ? "true" : "false";
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("initializeGame('" + turn_string + "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            webView.evaluateJavascript("updateStatus('" + stat + "')", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    GAME_STATE = RUNNING;
                                    if (!isTurn) makeMove("-1");
                                }
                            });
                        }
                    });
                }
            });
        }

    }

    @Override
    protected void onStop()
    {
        try {
            super.onStop();
//            ac.stopStreamingAudio();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
