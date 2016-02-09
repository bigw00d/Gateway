package ytech.prototype.osiris.gateway;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.content.Context;
import android.widget.Toast;

public class GcmIntentService extends IntentService {
	  private static final String TAG = "GcmIntentService";
	  public GcmIntentService() {
	    super("GcmIntentService");
	  }
	  @Override
	  protected void onHandleIntent(Intent intent) { 
	    Bundle extras = intent.getExtras();
	    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	    String messageType = gcm.getMessageType(intent);
	 
	    if (!extras.isEmpty()) {
	      if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
	        Log.d(TAG,"messageType: " + messageType + ",body:" + extras.toString());
	      } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
	        Log.d(TAG,"messageType: " + messageType + ",body:" + extras.toString());
	      } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
	        Log.d(TAG,"messageType: " + messageType + ",body:" + extras.toString());
	      }

	      String message = extras.toString();
		  sendBroadCast(message); 	    }
	    
	    GcmBroadcastReceiver.completeWakefulIntent(intent);
	  }

		protected void sendBroadCast(String message) {
			 
			Intent broadcastIntent = new Intent();
			broadcastIntent.putExtra("message", message);
			broadcastIntent.setAction("UPDATE_ACTION");
			getBaseContext().sendBroadcast(broadcastIntent);
		 
		}

}
