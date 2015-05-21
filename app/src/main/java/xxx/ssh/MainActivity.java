package xxx.ssh;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.maverick.ssh.PseudoTerminalModes;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshConnector;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    SocketTransport createTransport(String hostname, int port)
    {
        SocketTransport st = null;

        try {
            st = new SocketTransport(hostname, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return st;
    }

    class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            try {
                String hostname = "192.168.0.192";

                int port = 22;

                String username = "pi";

                /**
                 * Create an SshConnector instance
                 */
                SshConnector con = SshConnector.createInstance();

                // Verify server host keys using the users known_hosts file
                //con.setKnownHosts(new ConsoleKnownHostsKeyVerification());

                ((Ssh2Context)con.getContext(2)).setPreferredPublicKey("ssh-rsa");
                /**
                 * Connect to the host
                 */

                System.out.println("Connecting to " + hostname);

                SocketTransport transport;
                transport =  new SocketTransport(hostname, port);

                System.out.println("Creating SSH client");

                final SshClient ssh = con.connect(transport,
                        username);


                //((Ssh2Client)ssh).getAuthenticationMethods(username);

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
                com.maverick.ssh.PasswordAuthentication pwd = new com.maverick.ssh.
                        PasswordAuthentication();

                do {
                    System.out.print("Password: raspberry");
                    pwd.setPassword("raspberry");
                } while (ssh.authenticate(pwd) != SshAuthentication.COMPLETE
                        && ssh.isConnected());

                /**
                 * Start a session and do basic IO
                 */
                if (ssh.isAuthenticated()) {

                    // Some old SSH2 servers kill the connection after the first
                    // session has closed and there are no other sessions started;
                    // so to avoid this we create the first session and dont ever use it
                    final SshSession session = ssh.openSessionChannel();

                    // Use the newly added PseudoTerminalModes class to
                    // turn off echo on the remote shell
                    PseudoTerminalModes pty = new PseudoTerminalModes(ssh);
                    pty.setTerminalMode(PseudoTerminalModes.ECHO, false);

                    session.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);

                    session.startShell();

                    Thread t = new Thread() {
                        public void run() {
                            try {
                                int read;
                                while ((read = session.getInputStream().read()) > -1) {
                                    System.out.write(read);
                                    System.out.flush();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    };

                    t.start();
                    int read;
//                        byte[] buf = new byte[4096];
                    while((read = System.in.read()) > -1) {
                        session.getOutputStream().write(read);

                    }

                    session.close();
                }

                ssh.disconnect();
            } catch(Throwable t) {
                t.printStackTrace();
            }

            return null;
        }
    }

    public void onMyButtonClick(View view) {
        MyTask mt = new MyTask();
        mt.execute();
    }

}
