package com.team341.daisycv.communication.client;

import android.util.Log;
import com.team341.daisycv.communication.messages.HeartbeatMessage;
import java.util.concurrent.TimeUnit;

/**
 * Created by joshs on 8/6/2017.
 */

public class ConnectionThread extends ClientThread {

  public static final String LOGTAG = "ConnectionThread";
  private long mLastSentHearbeatTime = System.currentTimeMillis();
  private long mLastReceivedHearbeatTime = 0;

  public ConnectionThread(Client client) {
    super(client);
  }

  @Override
  public void run() {
    while (mClient.isEnabled()) {
      try {
        if ((mClient.getSocket() == null || !mClient.getSocket().isConnected()) && !mClient
            .isConnected()) {
          mClient.tryConnecting();
          Thread.sleep(250);
        }

        long now = System.currentTimeMillis();

        if ((now - mLastSentHearbeatTime) > 100) {
          Log.i(LOGTAG, "before offer");
          Log.i(LOGTAG, "messageQueue() size" + mClient.getMessageQueue().size());
          mClient.getMessageQueue()
              .offer(HeartbeatMessage.getInstance(), 100, TimeUnit.MILLISECONDS);
          mLastSentHearbeatTime = now;
        }

        Log.i(LOGTAG, "after offer");
        //Log.i(LOGTAG, "l sent hb time: " + mLastSentHearbeatTime);
        //Log.i(LOGTAG, "l received - sent hb time: " + (mLastReceivedHearbeatTime -
        //    mLastSentHearbeatTime));

        if (Math.abs(mLastReceivedHearbeatTime - mLastSentHearbeatTime) > 800 && mClient
            .isConnected()) {
          mClient.notifyDisconnected();
        }

        if (Math.abs(mLastReceivedHearbeatTime - mLastSentHearbeatTime) < 800 && !mClient
            .isConnected()) {
          mClient.notifyConnected();
        }

        Thread.sleep(100);

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized void updateLastReceivedHeartbeatTime(long time) {
    mLastReceivedHearbeatTime = time;
  }
}
