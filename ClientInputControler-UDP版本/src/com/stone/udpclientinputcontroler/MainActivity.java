package com.stone.udpclientinputcontroler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.stone.softkeyboard.LatinKeyboard;
import com.stone.softkeyboard.LatinKeyboardView;
import com.stone.udpclientinputcontroler.R;
import com.stone.widget.TouchPadView;
import com.stone.widget.TouchPadView.TouchPadListener;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends Activity implements
		KeyboardView.OnKeyboardActionListener, OnClickListener,
		OnLongClickListener, OnTouchListener, TouchPadListener {
	private final static String IP_REGLEX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
			+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
			+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
			+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

	private String mServerIP = "192.168.1.101";
	private int mServerPort = 9999;

	private TouchPadView mTouchPadView;

	private int mMouseAccuracy = 3;
	private EditText mCommand;

	private Button mSendButton;
	private Button mClickButton;
	private Button mRightClickButton;
	private Button mDoubleClickButton;
	private Button mUpButton;
	private Button mLeftButton;
	private Button mRightButton;
	private Button mDownButton;

	private ExecutorService mExecutors = Executors.newSingleThreadExecutor();

	private AutoReproductSocketRunnable mLongClickSocketRunnable;
	private boolean mLongClickBegin = false;

	private LatinKeyboardView mKeyboardView;
	private LatinKeyboard mQwertyKeyboard, mSymbolsKeyboard,
			mSymbolsShiftedKeyboard;

	private AlertDialog mDialog;
	private View mDialogContent = null;

	private DialogInterface.OnClickListener mPositiveClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if (which == DialogInterface.BUTTON_POSITIVE) {
				EditText ipEditText = (EditText) mDialogContent
						.findViewById(R.id.ip);
				EditText portEditText = (EditText) mDialogContent
						.findViewById(R.id.port);

				String IP = ipEditText.getText().toString();
				String port = portEditText.getText().toString();
				if (IP != null && port != null && IP.matches(IP_REGLEX)
						&& port.length() == 4) {
					mServerIP = IP;
					mServerPort = Integer.valueOf(port);

					// set step
					SeekBar seekBar = (SeekBar) mDialogContent
							.findViewById(R.id.sb_step);
					int step = seekBar.getProgress();
					if (step < 1) {
						step = 1;
						seekBar.setProgress(step);
					}
					String command = "#" + String.valueOf(step);
					if (command != null && command.matches("#[1-9]+[0-9]*")) {
						mExecutors.execute(new SocketRunnable(command,
								mServerIP, mServerPort));
						mMouseAccuracy = seekBar.getProgress();
					}
				} else {
					Toast.makeText(
							getApplicationContext(),
							MainActivity.this
									.getString(R.string.toast_invalid_ip),
							Toast.LENGTH_SHORT).show();
					mServerIP = "192.168.1.101";
					mServerPort = 8888;
				}
			}
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initAllWidgets();
		ShowConfigServerDialog();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mDialog != null && mDialog.isShowing())
			mDialog.dismiss();
	}

	private void initAllWidgets() {
		mCommand = (EditText) findViewById(R.id.msg);

		mSendButton = (Button) findViewById(R.id.bt_send);
		mClickButton = (Button) findViewById(R.id.bt_click);
		mRightClickButton = (Button) findViewById(R.id.bt_right_click);
		mDoubleClickButton = (Button) findViewById(R.id.bt_double_click);
		mUpButton = (Button) findViewById(R.id.bt_up);
		mLeftButton = (Button) findViewById(R.id.bt_left);
		mRightButton = (Button) findViewById(R.id.bt_right);
		mDownButton = (Button) findViewById(R.id.bt_down);

		// for click
		mSendButton.setOnClickListener(this);
		mClickButton.setOnClickListener(this);
		mRightButton.setOnClickListener(this);
		mDoubleClickButton.setOnClickListener(this);
		mUpButton.setOnClickListener(this);
		mLeftButton.setOnClickListener(this);
		mRightClickButton.setOnClickListener(this);
		mDownButton.setOnClickListener(this);

		// for long and touch listener
		mUpButton.setOnLongClickListener(this);
		mUpButton.setOnTouchListener(this);
		mLeftButton.setOnLongClickListener(this);
		mLeftButton.setOnTouchListener(this);
		mRightButton.setOnLongClickListener(this);
		mRightButton.setOnTouchListener(this);
		mDownButton.setOnLongClickListener(this);
		mDownButton.setOnTouchListener(this);

		mTouchPadView = (TouchPadView) findViewById(R.id.touch_pad);
		mTouchPadView.setTouchPadListener(this);

		mKeyboardView = (LatinKeyboardView) findViewById(R.id.keyboard);
		mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
		mQwertyKeyboard.setShifted(mShifted);
		mKeyboardView.setOnKeyboardActionListener(this);
		mKeyboardView.setKeyboard(mQwertyKeyboard);

		mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
		mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
	}

	private void setQwertyKeyboard() {
		mKeyboardView.setKeyboard(mQwertyKeyboard);
		mQwertyKeyboard.setShifted(mShifted);
	}

	private void setSymbolsKeyboard() {
		Keyboard currentKeyboard = mKeyboardView.getKeyboard();
		if (mSymbolsKeyboard == currentKeyboard) {
			mSymbolsKeyboard.setShifted(true);
			mKeyboardView.setKeyboard(mSymbolsShiftedKeyboard);
			mSymbolsShiftedKeyboard.setShifted(true);
		} else {
			mSymbolsShiftedKeyboard.setShifted(false);
			mKeyboardView.setKeyboard(mSymbolsKeyboard);
			mSymbolsKeyboard.setShifted(false);
		}
	}

	private void ShowConfigServerDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

		mDialogContent = getLayoutInflater().inflate(
				R.layout.config_server_dialog, null);
		SeekBar seekBar = (SeekBar) mDialogContent.findViewById(R.id.sb_step);
		seekBar.setProgress(mMouseAccuracy);

		mDialog = builder.setView(mDialogContent).setTitle("请设置服务器信息")
				.setPositiveButton("确定", mPositiveClickListener)
				.setNegativeButton("取消", null).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.menu_settings) {
			ShowConfigServerDialog();
		}

		return super.onOptionsItemSelected(item);
	}

	/*-
	 * For Keyboard BEGIN>>>
	 */
	private boolean mShifted = false;

	@Override
	public void onPress(int primaryCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRelease(int primaryCode) {
		// TODO Auto-generated method stub
		if (-1 == primaryCode) {// shift
			if ((LatinKeyboard) mKeyboardView.getKeyboard() == mQwertyKeyboard) {// Qwerty
																					// shift
				mShifted = !mShifted;
				mQwertyKeyboard.setShifted(mShifted);
				mKeyboardView.invalidateAllKeys();
			} else {// symbols shift
				setSymbolsKeyboard();
			}
		} else if (-2 == primaryCode) {// 123
			setSymbolsKeyboard();
		} else if (-22 == primaryCode) {// symbol abc
			setQwertyKeyboard();
		}
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		// TODO Auto-generated method stub
		if (-1 == primaryCode) {// shift
			return;
		}

		if (primaryCode == -2) {// key 123
			return;
		}

		// symbols keyboard begin
		if (primaryCode == -22) {// abc
			return;
		}

		if (primaryCode == -11) {// shift
			return;
		}

		if (primaryCode == -3) {// hide keyboard
			return;
		}
		// symbols keyboard end

		if (primaryCode == -5) {// delete key
			primaryCode = 0x08;
		} else if (primaryCode >= 97 && primaryCode <= 122) {// 'a'-'z'
			if (mShifted) {
				primaryCode -= 32;// 'A'-'Z'
			}
		}
		mExecutors.execute(new SocketRunnable("##" + (char) primaryCode,
				mServerIP, mServerPort));
	}

	@Override
	public void onText(CharSequence text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeLeft() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeRight() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeDown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeUp() {
		// TODO Auto-generated method stub

	}

	/*-
	 * For Keyboard END<<<
	 */

	/*-
	 * For Buttons BEGIN>>>
	 */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		if (id == R.id.bt_send) {// send
			String command = mCommand.getText().toString();
			if (command != null && command.length() > 0) {
				mExecutors.execute(new SocketRunnable("##" + command,
						mServerIP, mServerPort));
			} else {
				Toast.makeText(getApplicationContext(),
						"Please input command !", Toast.LENGTH_SHORT).show();
			}
			mCommand.setText("");
		} else if (id == R.id.bt_click) {// single click
			mExecutors.execute(new SocketRunnable(MouseAction.CLICK, mServerIP,
					mServerPort));
		} else if (id == R.id.bt_right_click) {// right click
			mExecutors.execute(new SocketRunnable(MouseAction.RIGHT_CLICK,
					mServerIP, mServerPort));
		} else if (id == R.id.bt_double_click) {// double click
			mExecutors.execute(new SocketRunnable(MouseAction.DOUBLE_CLICK,
					mServerIP, mServerPort));
		} else if (id == R.id.bt_up) {// up
			mExecutors.execute(new SocketRunnable(MouseAction.UP, mServerIP,
					mServerPort));
		} else if (id == R.id.bt_left) {// left
			mExecutors.execute(new SocketRunnable(MouseAction.LEFT, mServerIP,
					mServerPort));
		} else if (id == R.id.bt_right) {// right
			mExecutors.execute(new SocketRunnable(MouseAction.RIGHT, mServerIP,
					mServerPort));
		} else if (id == R.id.bt_down) {// down
			mExecutors.execute(new SocketRunnable(MouseAction.DOWN, mServerIP,
					mServerPort));
		}
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		mLongClickBegin = true;
		mLongClickSocketRunnable = null;

		int id = v.getId();
		if (id == R.id.bt_send) {// send
			// do nothing
		} else if (id == R.id.bt_click) {// single click
			// do nothing
		} else if (id == R.id.bt_right_click) {// right click
			// do nothing
		} else if (id == R.id.bt_double_click) {// double click
			// do nothing
		} else if (id == R.id.bt_up) {// up
			mLongClickSocketRunnable = new AutoReproductSocketRunnable(
					MouseAction.UP, mExecutors, mServerIP, mServerPort);
		} else if (id == R.id.bt_left) {// left
			mLongClickSocketRunnable = new AutoReproductSocketRunnable(
					MouseAction.LEFT, mExecutors, mServerIP, mServerPort);
		} else if (id == R.id.bt_right) {// right
			mLongClickSocketRunnable = new AutoReproductSocketRunnable(
					MouseAction.RIGHT, mExecutors, mServerIP, mServerPort);
		} else if (id == R.id.bt_down) {// down
			mLongClickSocketRunnable = new AutoReproductSocketRunnable(
					MouseAction.DOWN, mExecutors, mServerIP, mServerPort);
		}

		if (mLongClickSocketRunnable != null) {
			mExecutors.execute(mLongClickSocketRunnable);
		}

		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_CANCEL
				|| event.getAction() == MotionEvent.ACTION_UP) {
			if (mLongClickBegin) {
				mLongClickBegin = false;
				if (mLongClickSocketRunnable != null) {
					mLongClickSocketRunnable.disableReproduct();
				}
			}
		}

		return false;
	}

	/*-
	 * For Buttons END>>>
	 */

	/*-
	 * Some Inner classes BEGIN>>>
	 */
	private enum MouseAction {
		LEFT, RIGHT, UP, DOWN, CLICK, RIGHT_CLICK, DOUBLE_CLICK, SCROLL
	}

	private static class SocketRunnable implements Runnable {
		protected MouseAction mAction = null;
		protected String mCommandStr = null;
		protected String mDstHost;
		protected int mDstPort;

		public SocketRunnable(MouseAction action, String dstHost, int dstPort) {
			if (action == null) {
				throw new RuntimeException("Invalid MouseAction!");
			}

			mAction = action;
			mDstHost = dstHost;
			mDstPort = dstPort;
		}

		public SocketRunnable(String command, String dstHost, int dstPort) {
			if (command == null || command.length() < 1)
				throw new RuntimeException("Invalid Command!");

			mCommandStr = command;
			mDstHost = dstHost;
			mDstPort = dstPort;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (mAction != null) {
				sendServerMsg(mAction);
			} else {
				sendServerMsg(mCommandStr);
			}
		}

		public void sendServerMsg(String msg) {
//			try {
				sendUDPPacket(msg);
				
				/*Socket socket = new Socket(mDstHost, mDstPort);
				BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
						socket.getOutputStream()));
				br.write(msg);
				br.flush();
				br.close();*/
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

		public void sendServerMsg(MouseAction action) {
			String msg = null;
			switch (action) {
			case LEFT:
				msg = "l";
				break;
			case RIGHT:
				msg = "r";
				break;
			case UP:
				msg = "u";
				break;
			case DOWN:
				msg = "d";
				break;
			case CLICK:
				msg = "s";
				break;
			case DOUBLE_CLICK:
				msg = "t";
				break;
			case RIGHT_CLICK:
				msg = "m";
				break;
			case SCROLL:
				msg = "o";
				break;
			default:
				break;
			}

			if (msg != null)
				sendServerMsg(msg);
		}
		
		protected void sendUDPPacket(String msg) {
			Log.i("Jerry", "sendUDPPacket(): msg = " + msg);
			try {
				DatagramSocket socket = new  DatagramSocket (9998);
				InetAddress serverAddress = InetAddress.getByName(mDstHost); 
				byte data[] = msg.getBytes();
				DatagramPacket packet = 
						new DatagramPacket(data, data.length, serverAddress, mDstPort);
				socket.send(packet);
				socket.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static class AutoReproductSocketRunnable extends SocketRunnable {
		private volatile boolean mGoOn = true;
		private BufferedWriter mWriter = null;
		private ExecutorService mSocketExecutor;

		public AutoReproductSocketRunnable(MouseAction action,
				ExecutorService executor, String dstHost, int dstPort) {
			super(action, dstHost, dstPort);
			// TODO Auto-generated constructor stub

			if (executor == null)
				throw new RuntimeException("ExecutorService cannot be null !");
			mSocketExecutor = executor;
		}

		@SuppressWarnings("unused")
		public void enableReproduct() {
			mGoOn = true;
		}

		public void disableReproduct() {
			mGoOn = false;
		}

		@Override
		public void sendServerMsg(String msg) {
			// TODO Auto-generated method stub
//			try {
				sendUDPPacket(msg);
				
//				if (mWriter == null) {
//					Socket socket = new Socket(mDstHost, mDstPort);
//					mWriter = new BufferedWriter(new OutputStreamWriter(
//							socket.getOutputStream()));
//				}
//				mWriter.flush();
//				mWriter.write(msg);
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();

			if (mGoOn) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mSocketExecutor.execute(this);

			} else {
				try {
					if (mWriter != null)
						mWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/*-
	 * Some Inner classes END>>>
	 */

	private final static String TAG = "MainActivity";

	@Override
	public boolean onMove(int dx, int dy) { 
		// TODO Auto-generated method stub
		StringBuilder msg = new StringBuilder("###");
		
		if(dx>0){
			msg.append("r");
			msg.append(String.valueOf(dx));
		}else{
			msg.append("l");
			msg.append(String.valueOf(-dx));
		}
		
		if(dy>0){
			msg.append("d");
			msg.append(String.valueOf(dy));
		}else{
			msg.append("u");
			msg.append(String.valueOf(-dy));
		}
		
		Log.i(TAG, "onMove(): msg = " + msg.toString());
		mExecutors.execute(new SocketRunnable(msg.toString(), mServerIP, mServerPort));
		
		return false;
	}

}
