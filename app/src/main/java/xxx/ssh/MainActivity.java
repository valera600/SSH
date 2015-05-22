package xxx.ssh;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.maverick.ssh.ChannelOpenException;
import com.maverick.ssh.PseudoTerminalModes;
import com.maverick.ssh.Shell;
import com.maverick.ssh.ShellProcess;
import com.maverick.ssh.ShellTimeoutException;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshClientConnector;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.SshException;
import com.maverick.ssh.SshIOException;
import com.maverick.ssh.SshSession;
import com.maverick.ssh1.Ssh1Client;
import com.maverick.ssh2.Ssh2Context;
import com.sshtools.net.SocketTransport;
import com.sshtools.publickey.ConsoleKnownHostsKeyVerification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends ActionBarActivity {
    public SshClient ssh = null;
    public boolean up = false;
    public boolean left = false;
    public TextView tvInfo;
    Button btnUp = null;
    Button btnLeft = null;
    Button btnUpOff = null;
    Button btnLeftOff = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo = (TextView) findViewById(R.id.textView);
        btnUp = (Button) findViewById(R.id.buttonUp);
        btnUpOff = (Button) findViewById(R.id.buttonUpOff);
        btnLeft = (Button) findViewById(R.id.buttonLeft);
        btnLeftOff = (Button) findViewById(R.id.buttonLeftOff);

        View.OnTouchListener oclBtn = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (v.getId()) {
                    case R.id.buttonUp:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 2");
                                up = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 2");
                                up = false;
                                break;
                        }
                        break;
                    case R.id.buttonLeft:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 3");
                                left = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 3");
                                left = false;
                                break;
                        }
                        break;
                    default:
                        break;
                }
                Thread thr = new Thread(new ThreadSSH());
                thr.start();
                return false;
            }
        };

        btnUp.setOnTouchListener(oclBtn);
        btnLeft.setOnTouchListener(oclBtn);

        com.maverick.ssh.LicenseManager.addLicense("----BEGIN 3SP LICENSE----\r\n"
                + "Product : J2SSH Maverick\r\n"
                + "Licensee: home\r\n"
                + "Comments: valera\r\n"
                + "Type    : Evaluation License\r\n"
                + "Created : 20-May-2015\r\n"
                + "Expires : 04-Jul-2015\r\n"
                + "\r\n"
                + "378720442AD7AFDBD9FF0D7E356DDB3726C8F941F73461DB\r\n"
                + "9EA464FFDE8ED058DD243D641C0CA39A59C34D5F39DBF626\r\n"
                + "4A6B7861CE00DE15D79180416149437F0466391277AD9C5F\r\n"
                + "A6B99159B3B624F119190AE0EB84F693A7BEA5C942D96DA2\r\n"
                + "3B14B770034FFF022CFB302939A700B04348B624CF1D741C\r\n"
                + "6245D5119B975A2778590441933FA164275270BAB973A93D\r\n"
                + "----END 3SP LICENSE----\r\n");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            try {
                String hostname = "192.168.0.192";

                int port = 22;

                String username = "pi";

                /**
                 * Create an SshConnector instance
                 */
                SshClientConnector con = SshConnector.createInstance();

                /**
                 * Connect to the host
                 */
                ssh = con.connect(new SocketTransport(hostname,
                        port), username);

                /**
                 * Determine the version
                 */
                if (ssh instanceof Ssh1Client)
                    System.out.println(hostname + " is an SSH1 server");
                else
                    System.out.println(hostname + " is an SSH2 server");

                /**
                 * Authenticate the user using password authentication
                 */
                com.maverick.ssh.PasswordAuthentication pwd = new com.maverick.ssh.PasswordAuthentication();

                do {
                    System.out.print("Password: raspberry");
                    pwd.setPassword("raspberry");
                } while (ssh.authenticate(pwd) != SshAuthentication.COMPLETE
                        && ssh.isConnected());

                /**
                 * Start a session and do basic IO
                 */
                if (ssh.isAuthenticated()) {

                    /**
                     * Create a Shell
                     */
                    SshSession session = ssh.openSessionChannel();
                    session.executeCommand("ls");
                    session.close();
                }
                //ssh.disconnect();

            } catch (SshIOException e) {
                e.printStackTrace();
            } catch (ChannelOpenException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SshException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            tvInfo.setText("Connected!");
        }
    }

    class ThreadSSH implements Runnable {

        public void run() {
            updateGPIO updGPIO = new updateGPIO();
            Thread thr = new Thread(updGPIO);
            thr.start();
        }
    }

    class updateGPIO implements Runnable{
        public void run(){
            if(ssh.isAuthenticated()) {
                try {
                    SshSession session = ssh.openSessionChannel();
                    if(up)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio2/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio2/value");
                    session.close();
                    session = ssh.openSessionChannel();
                    if(left)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio3/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio3/value");
                    session.close();
                } catch (SshException | ChannelOpenException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onMyButtonClick(View view) {
        MyTask mt = new MyTask();
        mt.execute();
    }

    public void onDisconnectButtonClick(View view) {
        ssh.disconnect();
        tvInfo.setText("Disconnected!");
    }

}
