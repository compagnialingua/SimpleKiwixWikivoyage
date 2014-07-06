package org.kiwix.kiwixmobile;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public class LoadingActivity extends Activity
{
	
	private AsyncTask<?, ?, ?> loadingTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		
		loadingTask = new AsyncTask<Void, Void, Void>()
		{

			@Override
			protected Void doInBackground(Void... params)
			{
				File file = new File(getString(R.string.zim_path, Locale.getDefault().getLanguage().toLowerCase(Locale.ENGLISH)));
				if(!file.exists())
					file = new File(getString(R.string.zim_path, Locale.ENGLISH.getLanguage().toLowerCase(Locale.ENGLISH)));
				ZimContentProvider.setZimFile(file.getAbsolutePath());
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result)
			{
				loadingTask = null;
				finish();
				Intent intent = new Intent(LoadingActivity.this, KiwixMobileActivity.class);
				intent.putExtra("start", true);
				startActivity(intent);
			}
		}.execute();
	}
	
	@Override
	public void onBackPressed()
	{
		if(loadingTask != null)
			loadingTask.cancel(true);
		super.onBackPressed();
	}
}
