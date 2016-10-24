package wifi.bizzmark.com.wifidirectsend;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Button;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import wifi.bizzmark.com.wifidirectsend.Adapter.WifiAdapter;
import wifi.bizzmark.com.wifidirectsend.BroadcastReceiver.WifiDirectBroadcastReceiver;
import wifi.bizzmark.com.wifidirectsend.Service.DataTransferService;

public class WifiDirectSend extends AppCompatActivity {

    private Button btnSend;
    private TextView txtView;

    private RecyclerView mRecyclerView;
    private WifiAdapter mAdapter;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    // Connection info object.
    private WifiP2pInfo info;


    // For peers information.
    private List<HashMap<String, String>> peersshow = new ArrayList<HashMap<String, String>>();

    // All the peers.
    private List peers = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_send);


        initView();
        initIntentFilter();
        initReceiver();
        initEvents();
    }

    private void initView() {

        btnSend = (Button) findViewById(R.id.btnSend);
        txtView = (TextView) findViewById(R.id.txtSend);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mAdapter = new WifiAdapter(peersshow);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));

    }


    private void initIntentFilter() {

        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }


    private void initReceiver() {

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, Looper.myLooper(), null);

        WifiP2pManager.PeerListListener mPeerListListerner = new WifiP2pManager.PeerListListener() {

            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {

                Toast.makeText(getApplicationContext(),"WifiP2pManager.PeerListListener onPeersAvailable.",Toast.LENGTH_SHORT);
                peers.clear();
                peersshow.clear();

                Collection<WifiP2pDevice> aList = peersList.getDeviceList();
                peers.addAll(aList);

                for (int i = 0; i < aList.size(); i++) {

                    WifiP2pDevice a = (WifiP2pDevice) peers.get(i);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("name", a.deviceName);
                    map.put("address", a.deviceAddress);
                    peersshow.add(map);
                }

                mAdapter = new WifiAdapter(peersshow);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(WifiDirectSend.this));

                mAdapter.SetOnItemClickListener(new WifiAdapter.OnItemClickListener() {

                    @Override
                    public void OnItemClick(View view, int position) {

                        createConnect(peersshow.get(position).get("address"), peersshow.get(position).get("name"));
                    }

                    @Override
                    public void OnItemLongClick(View view, int position) {

                    }
                });
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo minfo) {

                Toast.makeText(getApplicationContext(),"ConnectionInfoListener onConnectionInfoAvailable.",Toast.LENGTH_SHORT);
                Log.i("xyz", "InfoAvailable is on");
                info = minfo;
                btnSend.setVisibility(View.VISIBLE);
            }
        };

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListerner, mInfoListener);
    }

    private void initEvents() {

        btnSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                discoverPeers();

                if(null == info) {

                    Toast.makeText(getApplicationContext(),"WifiP2pInfo is null.",Toast.LENGTH_SHORT);
                   // return;
                }

                Intent serviceIntent = new Intent(WifiDirectSend.this, DataTransferService.class);
                serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA);
                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());


                String sendText = txtView.getText().toString();
                serviceIntent.putExtra(DataTransferService.MESSAGE, sendText);

                Log.i("bizzmark", "owenerip is " + info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_PORT, 8888);
                WifiDirectSend.this.startService(serviceIntent);
                btnSend.setVisibility(View.GONE);
            }
        });


        mAdapter.SetOnItemClickListener(new WifiAdapter.OnItemClickListener() {

            @Override
            public void OnItemClick(View view, int position) {

                createConnect(peersshow.get(position).get("address"), peersshow.get(position).get("name"));
            }

            @Override
            public void OnItemLongClick(View view, int position) {

            }
        });
    }

    /*A demo base on API which you can connect android device by wifidirect,
    and you can send file or data by socket,what is the most important is that you can set
    which device is the client or service.*/

    private void createConnect(String address, final String name) {

        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        Log.i("bizzmark", address);

        config.deviceAddress = address;

        config.wps.setup = WpsInfo.PBC;
        Log.i("address", "MAC IS " + address);

        // Client app so not group owner.
        config.groupOwnerIntent = 0;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"WifiP2pManager connect success.", Toast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),"WifiP2pManager connect failure. Reason: " + reason, Toast.LENGTH_SHORT);
            }
        });
    }


    private void discoverPeers() {

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"WifiP2pManager discoverPeers success.", Toast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),"WifiP2pManager discoverPeers failure. Reason: " + reason, Toast.LENGTH_SHORT);
            }
        });
    }

}
