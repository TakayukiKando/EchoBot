package org.xgmtk.xmpp.sample;

import java.io.*;
import java.text.DateFormat;
import java.util.*;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

public class EchoBot implements ChatManagerListener, MessageListener{
	static EchoBot BOT;
	private static final String QUIT_COMMAND = "quit";
	private static final File SETTING_FILE = new File("resource/echo.properties");
	private static final Timer TIMER = new Timer();
	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
	
	private final XMPPConnection connection;
	private List<Chat> chats;
	public EchoBot(String service, String id, String passwd) throws XMPPException {
		this.chats = new ArrayList<Chat>();
		
		//Debug window will be opened, if recover this line from commented out.
		XMPPConnection.DEBUG_ENABLED = true;
		this.connection = new XMPPConnection(service);
		this.connection.connect();
		this.connection.login(id, passwd, EchoBot.class.getSimpleName());		
		this.connection.getChatManager().addChatListener(this);
		
		this.setTimeSignal(5*MINUTE);
		this.setPresenceUpdater(30*SECOND);
		System.out.println("Echo back service started.(user: \""+this.connection.getUser()+"\")");
	}

	private void setPresenceUpdater(long interval) {
		TIMER.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
				String status = "Date: "+dateTimeFormat.format(Calendar.getInstance().getTime());
				sendPresence(status);
			}
		}, Calendar.getInstance().getTime(), interval);
	}
	
	private void setTimeSignal(long interval) {
		Calendar local_calendar =  Calendar.getInstance();
		local_calendar.set(Calendar.MINUTE, 0);
		local_calendar.set(Calendar.SECOND, 0);
		local_calendar.set(Calendar.MILLISECOND, 0);
		Date startTime = local_calendar.getTime();
		TIMER.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
				String message = "* Time signal: "+dateTimeFormat.format(Calendar.getInstance().getTime());
				sendMessageToAll(message);
			}
		}, startTime, interval);
		DateFormat dateTimeInstance = DateFormat.getDateTimeInstance();
		System.out.println("Start time signale(interval: "+((double)interval / MINUTE)+" minutes) at : "+dateTimeInstance.format(startTime));
	}
	
	public void sendMessageToAll(String message) {
		for(Chat c : this.chats){
			try {
				c.sendMessage(message);
			} catch (XMPPException e) {
				System.out.flush();
				System.err.println("** Failed to send a time signal message to \""+c.getParticipant()+"\". **");
				e.printStackTrace();
			}
		}
	}

	public void sendPresence(String status) {
		Presence.Mode m = Presence.Mode.chat;
		Presence.Type type = Presence.Type.available;
		Presence p = new Presence(type, status, 1, m);
		this.connection.sendPacket(p);
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		chat.addMessageListener(this);
		this.chats.add(chat);
	}

	@Override
	public void processMessage(Chat chat, Message message) {
		String sender = chat.getParticipant();
		System.out.println("Recieved a message from: \""+sender+"\", message text:\""+message.getBody());
		try {
			chat.sendMessage(message.getBody());
		} catch (XMPPException e) {
			System.out.flush();
			System.err.println("** Failed to send a echo-back message to \""+chat.getParticipant()+"\". **");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception{
		Properties settings = loadSettings();
		String service = settings.getProperty("service");
		String id = settings.getProperty("id");
		String passwd = settings.getProperty("passwd");
		
		BOT = new EchoBot(service, id, passwd);
		
		System.out.println("Type \""+QUIT_COMMAND+"\" and enter key to quit");
		BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
		String lineInput = lineReader.readLine();
		while(lineInput != null){
			if(lineInput.equals(QUIT_COMMAND)){
				break;
			}
			lineInput = lineReader.readLine();
		}
		System.exit(0);
	}

	private static Properties loadSettings() throws FileNotFoundException,
			IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(SETTING_FILE));
		Properties settings = new Properties();
		settings.load(is);
		is.close();
		return settings;
	}
}
