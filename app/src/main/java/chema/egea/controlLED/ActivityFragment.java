package chema.egea.controlLED;

import java.net.URI;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.alvinalexander.twitterclient3.R;

public class ActivityFragment extends Fragment
{

    private TextView datosServidor;
    private Switch interruptorFunction;
    private Switch interruptorVariable;


    boolean inOut = false;
    boolean onOff = false;
    
    private static final String TAG = "AsyncTestFragment";

    // get some fake data
    private static final String TEST_URL                   = "http://192.168.1.100:8000/*";
    private static final String GPIO_VALUE_URL             = "http://192.168.1.100:8000/GPIO/21/value";
    private static final String GPIO_FUNC_URL              = "http://192.168.1.100:8000/GPIO/21/function";

    private static final String ACTION_FOR_INTENT_CALLBACK = "THIS_IS_A_UNIQUE_KEY_WE_USE_TO_COMMUNICATE";

    ProgressDialog progress;

    //////////////////////////////////////////////////
    //CONTROL DE TIEMPO

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable()
    {

        @Override
        public void run()
        {
            actualizarInterfaz();
            timerHandler.postDelayed(this, 1000);
        }
    };
    //////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.myfragment_ui, container, false);

    }

    /**
     * any code to access activity fields must be handled in this method.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        timerHandler.postDelayed(timerRunnable, 1000);

        datosServidor = (TextView)getActivity().findViewById(R.id.TV_DatosActualizados);
        interruptorFunction = (Switch)getActivity().findViewById(R.id.switchinout);
        interruptorVariable = (Switch)getActivity().findViewById(R.id.switchonoff);


        interruptorFunction.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    interruptorVariable.setEnabled(true);
                }
                else
                {
                    interruptorVariable.setEnabled(false);
                }
                setFuncState();
            }
        });
        interruptorVariable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (interruptorFunction.isChecked())
                {
                    setVariableState();
                }
            }
        });

    }

    private void getFuncState()
    {
        // the request
        try {
            HttpGet httpGet = new HttpGet(new URI(GPIO_FUNC_URL));
            httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials("webiopi", "raspberry"), "UTF-8", false));
            RestTask task = new RestTask(getActivity(), ACTION_FOR_INTENT_CALLBACK);
            task.execute(httpGet);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void getVariableState()
    {
        // the request
        try
        {
            HttpGet httpGet = new HttpGet(new URI(GPIO_VALUE_URL));
            httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials("webiopi", "raspberry"), "UTF-8", false));
            RestTask task = new RestTask(getActivity(), ACTION_FOR_INTENT_CALLBACK);
            task.execute(httpGet);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }

    }

    private void setFuncState()
    {
        try
        {
            HttpPost httpPost = new HttpPost();
            if (interruptorFunction.isChecked())
            {
                httpPost.setURI(new URI(GPIO_FUNC_URL+"/out"));
            }
            else
            {
                httpPost.setURI(new URI(GPIO_FUNC_URL+"/in"));
            }
            httpPost.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials("webiopi", "raspberry"), "UTF-8", false));
            RestTask task = new RestTask(getActivity(), ACTION_FOR_INTENT_CALLBACK);
            task.execute(httpPost);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setVariableState()
    {
        try
        {
            HttpPost httpPost = new HttpPost();
            if (interruptorVariable.isChecked())
            {
                httpPost.setURI(new URI(GPIO_VALUE_URL+"/1"));
            }
            else
            {
                httpPost.setURI(new URI(GPIO_VALUE_URL+"/0"));
            }
            httpPost.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials("webiopi","raspberry"), "UTF-8",false));
            RestTask task = new RestTask(getActivity(), ACTION_FOR_INTENT_CALLBACK);
            task.execute(httpPost);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }
    }

    public void actualizarInterfaz()
    {
        getFuncState();
        getVariableState();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(ACTION_FOR_INTENT_CALLBACK));
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        getActivity().unregisterReceiver(receiver);
    }

    /**
     * Our Broadcast Receiver. We get notified that the data is ready, and then we
     * put the content we receive (a string) into the TextView.
     */
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // clear the progress indicator
            if (progress != null)
            {
                progress.dismiss();
            }
            String response = intent.getStringExtra(RestTask.HTTP_RESPONSE);
            Log.i(TAG, "RESPONSE = " + response);

            if (response.equals("IN") && inOut == true)
            {
                inOut = false;
                interruptorFunction.setChecked(inOut);

                String info = datosServidor.getText().toString();
                info ="Function: IN\n"+info;
                datosServidor.setText(info);
            }
            else if (response.equals("OUT")&& inOut == false)
            {
                inOut = true;
                interruptorFunction.setChecked(inOut);

                String info = datosServidor.getText().toString();
                info ="Function: OUT\n"+info;
                datosServidor.setText(info);
            }
            if (response.equals("0") && onOff == true)
            {
                onOff = false;
                interruptorVariable.setChecked(onOff);

                String info = datosServidor.getText().toString();
                info ="Variable: 0\n"+info;
                datosServidor.setText(info);
            }
            else if (response.equals("1") && onOff == false)
            {
                onOff = true;
                interruptorVariable.setChecked(onOff);


                String info = datosServidor.getText().toString();
                info ="Variable: 1\n"+info;
                datosServidor.setText(info);
            }

        }
    };



}







