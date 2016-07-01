/*
 * The NoDemoModeScreen activity is simply a way to modally tell the user that Demo Mode is not supported by this app.
 */
package com.valentine.hello_v1;

import com.valentine.hello_v1.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class NoDemoModeScreen extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nodemomode);
    }
	
	public void onNoDemoModeOK (View view)
	{
		Intent response = new Intent();
		setResult(Activity.RESULT_OK,response);		
		finish();
	}

}
