/*Bug: Sending file
 * 
 * 
 */
//Commands:
//SEND_FILE filename
//CREATE_ROOM room_name
//INVITE ip TO room_name
//ROOM room_name: words

package multi_chat4;

import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class MyFrame extends java.awt.Frame {
	private static final long serialVersionUID = 1L;

	DatagramSocket sendSocket;

	TextField tf = new TextField(20);
	TextArea ta = new TextArea();
	java.awt.Button b_send = new java.awt.Button("send");
	Map<String, ArrayList<String>> ownedRoomList = new HashMap<String, ArrayList<String>>();
	Map<String, String> joinedRoomList = new HashMap<String, String>();
	Syn syn = new Syn();

	public MyFrame(String nam) {

		super(nam);
		add("North", tf);
		add("Center", ta);
		add("South", b_send);
		setSize(350, 350);
		b_send.setActionCommand("press");
		b_send.addActionListener(new MyActionListener());
		// getRootPane().setDefaultButton(b_send);
		setVisible(true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {

				dispose();
				System.exit(0);
			}
		});

		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

	}

	class MyActionListener implements ActionListener {// send data
		String dest_ip = "192.168.127.255";

		public void actionPerformed(ActionEvent e) {
			String com = e.getActionCommand();
			if (com.equals("press")) {
				System.out.println("clicked!");
				String content = tf.getText();
				tf.setText(null);
				DatagramPacket dp;
				byte[] buf = content.getBytes();
				ta.append("Myself: " + content + "\n");

				if (content.contains("ROOM ") && content.contains(": ")) {
					String room_name = content.substring(content.indexOf("ROOM ") + 5, content.indexOf(": "));
					String words = content.substring(content.indexOf(": ") + 2);
					if (ownedRoomList.containsKey(room_name)) {
						for (String member_ip : ownedRoomList.get(room_name)) {
							try {
								buf = ("ROOM " + room_name + ": " + words).getBytes();
								dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(member_ip), 6666);
								sendSocket.send(dp);
							} catch (UnknownHostException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					} else if (joinedRoomList.containsKey(room_name)) {
						try {
							buf = ("ROOM " + room_name + ": " + words).getBytes();
							dp = new DatagramPacket(buf, buf.length,
									InetAddress.getByName(joinedRoomList.get(room_name)), 6666);
							sendSocket.send(dp);

						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} else {
						ta.append("You have neither created nor entered this room!");
					}
				}

				else if (content != "") {

					try {

						dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(dest_ip), 6666);
						sendSocket.send(dp);
					} catch (UnknownHostException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					if (content.contains("SEND_FILE ")&&content.contains("TO ")) {
						String send_file_path =  content.substring(content.indexOf("SEND_FILE ") + 10, content.indexOf("TO "));
						String file_dest_ip=content.substring(content.indexOf("TO ")+3);
						FileInputStream fis;
						try {
							fis = new FileInputStream(send_file_path);
							int size_f = fis.available();
							System.out.println(size_f);
							 buf= new byte[1024];//byte[] buf_file = new byte[1024];
							// int len;
							for (; fis.read(buf) != -1;) {
								dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(file_dest_ip),
										6666);
								sendSocket.send(dp);
								syn.myWait();

							}
							// fis.read(buf_file);
							fis.close();
							buf = "EOF".getBytes();
							dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(file_dest_ip), 6666);
							sendSocket.send(dp);
							syn.myWait();
//							dp = new DatagramPacket(buf_file, buf_file.length, InetAddress.getByName(dest_ip), 6666);
//							sendSocket.send(dp);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
//						 catch (InterruptedException e1) {
//							e1.printStackTrace();
//						}

						// fileSend(dest_file_path);
					} else if (content.contains("CREATE_ROOM")) {
						String room_name = content.replaceAll("CREATE_ROOM ", "");
						ownedRoomList.put(room_name, new ArrayList<String>());
						ta.append("You have created room \"" + room_name + "\".\n");
					} else if (content.contains("INVITE ") && content.contains("TO ")) {
						String invited_ip = content.substring((content.indexOf("INVITE ") + 7),
								content.indexOf("TO ") - 1);
						String target_room = content.substring((content.indexOf("TO ") + 3));
						if (ownedRoomList.containsKey(target_room)) {
							buf = ("INVITE_TO " + target_room).getBytes();
							try {

								dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(invited_ip), 6666);
								sendSocket.send(dp);
							} catch (UnknownHostException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							ta.append("You have invited " + invited_ip + " to room " + target_room + "\n");
						}
					}

				}

			}
		}

	}

	class Rece implements Runnable {
		// MyFrame myFrame;
		private DatagramSocket ds;

		public Rece(DatagramSocket s) {
			ds = s;
			// myFrame = mf;
		}

		public void run() {
			// receive data
			try {
				while (true) {
					byte[] buf = new byte[1024];

					DatagramPacket dp = new DatagramPacket(buf, buf.length);
					ds.receive(dp);
					// notifyAll();
					String sender_ip = dp.getAddress().getHostAddress();
					String my_ip = InetAddress.getLocalHost().getHostAddress();
					if (!sender_ip.equals(my_ip)) {// !sender_ip.equals(my_ip)

						String data = new String(dp.getData(), 0, dp.getLength());
						if (data.contains("SEGMENT_ACK")) {
							syn.myNotify();
							ta.append("SEGMENT_ACK received\n");
						} else {
							ta.append(sender_ip + ": " + data + "\n");
							if (data.contains("SEND_FILE")) {
								// String send_path = data.replaceAll(" ", "").replaceAll("FILE", "");
								// int index_of_last_slash = send_path.lastIndexOf("\\");
								String save_path = "new_file.txt";// "C:\\" + send_path.substring(index_of_last_slash +
																	// 1);TODO
								System.out.println(save_path);
								FileOutputStream fos = new FileOutputStream(save_path);

								// String dest_file_path = data.replaceAll(" ", "").replaceAll("SEND_FILE", "");

								while (true) {
									buf = new byte[1024];
									dp = new DatagramPacket(buf, buf.length);
									ds.receive(dp);
									String data_file = new String(dp.getData(), 0, dp.getLength());

									// System.out.println(data_file.substring(1022, 1023));
									if (data_file.equals("EOF")) {
										//byte[] new_buf = new byte[1024];
										buf = ("SEGMENT_ACK").getBytes();
										dp = new DatagramPacket(buf, buf.length,
												InetAddress.getByName(sender_ip), 6666);
										sendSocket.send(dp);
										ta.append("SEGMENT_ACK sent\n");

										break;
									} else {
										// byte[] buf_file = data_file.getBytes();
										fos.write(dp.getData(), 0, dp.getLength());

										//byte[] new_buf = new byte[1024];
										buf = ("SEGMENT_ACK").getBytes();
										dp = new DatagramPacket(buf, buf.length,
												InetAddress.getByName(sender_ip), 6666);
										sendSocket.send(dp);
										ta.append("SEGMENT_ACK sent\n");
									}
								}
								fos.close();
								ta.append("FILE RECEIVED!\n");
							}

							else if (data.contains("INVITE_TO ")) {
								String room_name = data.substring(data.indexOf("INVITE_TO ") + 10);
								joinedRoomList.put(room_name, sender_ip);
								buf = ("INVITE_CONFIRM " + room_name).getBytes();

								try {
									dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(sender_ip), 6666);
									sendSocket.send(dp);
								} catch (UnknownHostException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								}

								ta.append(sender_ip + " has invited you to room \"" + room_name + "\"\n");
							}

							else if (data.contains("INVITE_CONFIRM ")) {
								String room_name = data.substring(data.indexOf("INVITE_CONFIRM ") + 15);
								ownedRoomList.get(room_name).add(sender_ip);// a member has entered the room
								ta.append(sender_ip + " has entered room \"" + room_name + "\"\n");
							}

							else if (data.contains("ROOM ") && data.contains(": ")) {
								String room_name = data.substring(data.indexOf("ROOM ") + 5, data.indexOf(": "));
								String words = data.substring(data.indexOf(": ") + 2);
								if (ownedRoomList.containsKey(room_name)) {
									for (String member_ip : ownedRoomList.get(room_name)) {
										if (member_ip.equals(sender_ip)) {
											continue;
										} else {
											try {
												buf = ("ROOM " + room_name + ": " + words).getBytes();
												dp = new DatagramPacket(buf, buf.length,
														InetAddress.getByName(member_ip), 6666);
												sendSocket.send(dp);
											} catch (UnknownHostException e1) {
												e1.printStackTrace();
											} catch (IOException e1) {
												e1.printStackTrace();
											}
										}
									}
								}
							}

						}
					}

				}
			} catch (Exception e) {
				throw new RuntimeException();
			}
		}
	}

	class Syn {
		synchronized void myWait() {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		synchronized void myNotify() {
			notify();
		}

	}

}

public class MultiChat4 {

	public static void main(String[] args) {
		MyFrame mf = new MyFrame("Jojo's Chat Program");

		DatagramSocket rec_soc;
		try {
			rec_soc = new DatagramSocket(6666);
			new Thread(mf.new Rece(rec_soc)).start();
		} catch (SocketException e) {

			e.printStackTrace();
		}

	}
}
