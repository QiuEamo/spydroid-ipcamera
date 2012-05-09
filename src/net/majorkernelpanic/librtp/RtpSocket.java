/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.librtp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

import net.majorkernelpanic.spydroid.SpydroidActivity;
import android.util.Log;



public class RtpSocket {

	private DatagramSocket usock;
	private DatagramPacket upack;
	
	private byte[] buffer;
	private int seq = 0;
	private boolean upts = false;
	
	public static final int headerLength = 12;
	
	public RtpSocket(byte[] buffer, InetAddress dest, int dport) {
		
		this(buffer);
		
		upack.setPort(dport);
		upack.setAddress(dest);
		
	}
	
	
	
	public RtpSocket(byte[] buffer) {
		
		this.buffer = buffer;
		
		/*							     Version(2)  Padding(0)					 					*/
		/*									 ^		  ^			Extension(0)						*/
		/*									 |		  |				^								*/
		/*									 | --------				|								*/
		/*									 | |---------------------								*/
		/*									 | ||  -----------------------> Source Identifier(0)	*/
		/*									 | ||  |												*/
		buffer[0] = (byte) Integer.parseInt("10000000",2);
		
		/* Payload Type */
		buffer[1] = (byte) 96;
		
		/* Byte 2,3        ->  Sequence Number                   */
		/* Byte 4,5,6,7    ->  Timestamp                         */
		
		/* Byte 8,9,10,11  ->  Sync Source Identifier            */
		setLong((new Random()).nextLong(),8,12);
		
		try {
			usock = new DatagramSocket();
		} catch (SocketException e) {
			
		}
		upack = new DatagramPacket(buffer, 1);

	}

	public void close() {
		usock.close();
	}
	
	public void setSSRC(long ssrc) {
		setLong(ssrc,8,12);
	}
	
	public void setDestination(InetAddress dest, int dport) {
		upack.setPort(dport);
		upack.setAddress(dest);
	}
	
	public byte[] getBuffer() {
		return buffer;
	}
	
	public int getLocalPort() {
		return usock.getLocalPort();
	}
	
	/* Send RTP packet over the network */
	public void send(int length) {
		
		updateSequence();
		upack.setLength(length);
		
		try {
			usock.send(upack);
		} catch (IOException e) {
			Log.e(SpydroidActivity.TAG,"Send failed");
		}
		
		if (upts) {
			upts = false;
			buffer[1] -= 0x80;
		}
		
	}
	
	private void updateSequence() {
		setLong(++seq, 2, 4);
	}
	
	public void updateTimestamp(long timestamp) {
		setLong(timestamp, 4, 8);
	}
	
	public void markNextPacket() {
		upts = true;
		buffer[1] += 0x80; // Mark next packet
	}
	
	public boolean isMarked() {
		return upts;
	}
	
	// Call this only one time !
	public void markAllPackets() {
		buffer[1] += 0x80;
	}
	
	private void setLong(long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buffer[end] = (byte) (n % 256);
			n >>= 8;
		}
	}	
	
}
