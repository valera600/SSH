package xxx.ssh;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.maverick.ssh.ChannelOpenException;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshClientConnector;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.SshException;
import com.maverick.ssh.SshSession;
import com.maverick.ssh1.Ssh1Client;
import com.sshtools.net.SocketTransport;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SshClient ssh = null;
    private boolean up = false;
    private boolean left = false;
    private boolean down = false;
    private boolean right = false;
    private boolean manualControl = true;
    private boolean wasChanged = false;
    private TextView tvInfo;
    private Button btnUp = null;
    private Button btnLeft = null;
    private Button btnDown = null;
    private Button btnRight = null;
    private SensorManager msensorManager = null; //Менеджер сенсоров аппрата

    private float[] rotationMatrix;     //Матрица поворота
    private float[] accelData;           //Данные с акселерометра
    private float[] magnetData;       //Данные геомагнитного датчика
    private float[] OrientationData; //Матрица положения в пространстве

    private TextView xzView;
    private TextView zyView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo = (TextView) findViewById(R.id.textView);
        btnUp = (Button) findViewById(R.id.buttonUp);
        btnDown = (Button) findViewById(R.id.buttonDown);
        btnLeft = (Button) findViewById(R.id.buttonLeft);
        btnRight = (Button) findViewById(R.id.buttonRight);
        Button btnTypeControl = (Button) findViewById(R.id.buttonChangeTypeControl);
        Button btnConnect = (Button) findViewById(R.id.buttonConnect);
        Button btnDisconnect = (Button) findViewById(R.id.buttonDisconnect);

        msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        rotationMatrix = new float[16];
        accelData = new float[3];
        magnetData = new float[3];
        OrientationData = new float[3];

        xzView = (TextView) findViewById(R.id.tvY);  // Наши текстовые поля для вывода показаний
        zyView = (TextView) findViewById(R.id.tvZ);  //

        //листенер для смены управления
        View.OnClickListener oclTypeControl = new View.OnClickListener() {
            public void onClick(View v){
                if(manualControl)
                {
                    manualControl = false;
                    btnUp.setVisibility(View.INVISIBLE);
                    btnLeft.setVisibility(View.INVISIBLE);
                    btnDown.setVisibility(View.INVISIBLE);
                    btnRight.setVisibility(View.INVISIBLE);
                    turnOnSensor();
                    xzView.setVisibility(View.VISIBLE);
                    zyView.setVisibility(View.VISIBLE);
                    stopMotion();
                }
                else
                {
                    manualControl = true;
                    btnUp.setVisibility(View.VISIBLE);
                    btnLeft.setVisibility(View.VISIBLE);
                    btnDown.setVisibility(View.VISIBLE);
                    btnRight.setVisibility(View.VISIBLE);
                    xzView.setVisibility(View.INVISIBLE);
                    zyView.setVisibility(View.INVISIBLE);
                    turnOffSensor();
                    stopMotion();
                }
            }
        };

        //листенер для ручного управления
        View.OnTouchListener oclBtn = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (v.getId()) {
                    //up
                    case R.id.buttonUp:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 2");
                                if(!down)
                                    up = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 2");
                                up = false;
                                break;
                        }
                        break;
                    //left
                    case R.id.buttonLeft:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 3");
                                if(!right)
                                    left = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 3");
                                left = false;
                                break;
                        }
                        break;
                    //down
                    case R.id.buttonDown:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 4");
                                if(!up)
                                    down = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 4");
                                down = false;
                                break;
                        }
                        break;
                    //right
                    case R.id.buttonRight:
                        switch (motionEvent.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                tvInfo.setText("1 17");
                                if(!left)
                                    right = true;
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                tvInfo.setText("0 17");
                                right = false;
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

        View.OnClickListener oclDisconnect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    ssh.disconnect();
                    tvInfo.setText("Disconnected!");
            }
        };


        View.OnClickListener oclConnect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyTask mt = new MyTask();
                mt.execute();
            }
        };

        btnUp.setOnTouchListener(oclBtn);
        btnLeft.setOnTouchListener(oclBtn);
        btnDown.setOnTouchListener(oclBtn);
        btnRight.setOnTouchListener(oclBtn);

        btnTypeControl.setOnClickListener(oclTypeControl);
        btnConnect.setOnClickListener(oclConnect);
        btnDisconnect.setOnClickListener(oclDisconnect);

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

    //connection
    private class MyTask extends AsyncTask<Void, Void, Void> {
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

            } catch (SshException | IOException | ChannelOpenException e) {
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

    private class ThreadSSH implements Runnable {

        public void run() {
            updateGPIO updGPIO = new updateGPIO();
            Thread thr = new Thread(updGPIO);
            thr.start();
        }
    }

    private class updateGPIO extends Thread implements Runnable {

        public void run(){
            if(ssh.isAuthenticated()) {
                try {
                    //up
                    SshSession session = ssh.openSessionChannel();
                    if(up)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio2/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio2/value");
                    session.close();
                    //left
                    session = ssh.openSessionChannel();
                    if(left)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio3/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio3/value");
                    session.close();
                    //down
                    session = ssh.openSessionChannel();
                    if(down)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio4/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio4/value");
                    session.close();
                    //right
                    session = ssh.openSessionChannel();
                    if(right)
                        session.executeCommand("echo 1 > /sys/class/gpio/gpio17/value");
                    else
                        session.executeCommand("echo 0 > /sys/class/gpio/gpio17/value");
                    session.close();
                } catch (SshException | ChannelOpenException e) {
                    e.printStackTrace();
                }
            }
            this.interrupt();
        }

    }

    //orientation
    @Override
    protected void onResume() {
        super.onResume();
        turnOnSensor();
    }

    private void turnOnSensor()
    {
        if (!manualControl) {
            msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void turnOffSensor()
    {
        if(!manualControl)
            msensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        turnOffSensor();
    }

    private void loadNewSensorData(SensorEvent event) {
        final int type = event.sensor.getType(); //Определяем тип датчика
        if (type == Sensor.TYPE_ACCELEROMETER) { //Если акселерометр
            accelData = event.values.clone();
        }

        if (type == Sensor.TYPE_MAGNETIC_FIELD) { //Если геомагнитный датчик
            magnetData = event.values.clone();
        }
    }

    public void onSensorChanged(SensorEvent event) {
        loadNewSensorData(event); // Получаем данные с датчика
        SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData); //Получаем матрицу поворота
        SensorManager.getOrientation(rotationMatrix, OrientationData); //Получаем данные ориентации устройства в пространстве

        //Выводим результат
        long y = Math.round(Math.toDegrees(OrientationData[1]));
        long z = Math.round(Math.toDegrees(OrientationData[2]));
        xzView.setText(String.valueOf(y));
        zyView.setText(String.valueOf(z));

        //up
        if (y >= 20) {
            if (!up) {
                wasChanged = true;
                up = true;
            }
        }
        //down
        if (y <= -20)
        {
            if(!down)
            {
                wasChanged = true;
                down = true;
            }
        }
        //non up and non down
        if(y > -20 && y < 20)
        {
            if(up || down)
            {
                wasChanged = true;
                up = false;
                down = false;
            }
        }
        //right
        if(z >= 20)
        {
            if(!right)
            {
                wasChanged = true;
                right = true;
            }
        }
        //left
        if(z <= -20)
        {
            if(!left)
            {
                wasChanged = true;
                left = true;
            }
        }
        //non right and non left
        if(z > -20 && z < 20)
        {
            if(left || right)
            {
                wasChanged = true;
                left = false;
                right = false;
            }
        }
        //apply changes
        if(wasChanged)
        {
            wasChanged = false;
            Thread thr = new Thread(new ThreadSSH());
            thr.start();
        }
    }

    private void stopMotion()
    {
        up = false;
        down = false;
        left = false;
        right = false;
        Thread thr = new Thread(new ThreadSSH());
        thr.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { //Изменение точности показаний датчика
    }
}
