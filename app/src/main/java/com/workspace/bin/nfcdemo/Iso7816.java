

package com.workspace.bin.nfcdemo;

import android.annotation.TargetApi;
import android.nfc.tech.IsoDep;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Iso7816 {
	public static final byte[] EMPTY = { 0 };

	protected byte[] data;



	protected Iso7816(byte[] bytes) {
		data = (bytes == null) ? Iso7816.EMPTY : bytes;
	}


	public boolean match(byte[] bytes, int start) {
		final byte[] data = this.data;
		if (data.length <= bytes.length - start) {
			for (final byte v : data) {
				if (v != bytes[start++])
					return false;
			}
		}
		return true;
	}



	public int size() {
		return data.length;
	}

	public byte[] getBytes() {
		return data;
	}

	@Override
	public String toString() {
		return Util.toHexString(data, 0, data.length);
	}

	public final static class ID extends Iso7816 {
		public ID(byte[] bytes) {
			super(bytes);
		}
	}

	public final static class Response extends Iso7816 {
		public static final byte[] EMPTY = {};
		public static final byte[] ERROR = { 0x6F, 0x00 }; // SW_UNKNOWN

		public Response(byte[] bytes) {
			super((bytes == null || bytes.length < 2) ? Response.ERROR : bytes);
		}

		public short getSw12() {
			final byte[] d = this.data;
			int n = d.length;
			return (short) ((d[n - 2] << 8) | (0xFF & d[n - 1]));
		}

		public boolean isOkey() {
			return equalsSw12(SW_NO_ERROR);
		}

		public boolean equalsSw12(short val) {
			return getSw12() == val;
		}

		public int size() {
			return data.length - 2;
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public byte[] getBytes() {
			return isOkey() ? Arrays.copyOfRange(data, 0, size())
					: Response.EMPTY;
		}
	}

	public final static class BerT extends Iso7816 {


		public static int test(byte[] bytes, int start) {
			int len = 1;
			if ((bytes[start] & 0x1F) == 0x1F) {
				while ((bytes[start + len] & 0x80) == 0x80)
					++len;

				++len;
			}
			return len;
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public static BerT read(byte[] bytes, int start) {
			return new BerT(Arrays.copyOfRange(bytes, start,
					start + test(bytes, start)));
		}


		public BerT(byte[] bytes) {
			super(bytes);
		}

		public boolean hasChild() {
			return ((data[0] & 0x20) == 0x20);
		}
	}

	public final static class BerL extends Iso7816 {
		private final int val;

		public static int test(byte[] bytes, int start) {
			int len = 1;
			if ((bytes[start] & 0x80) == 0x80) {
				len += bytes[start] & 0x07;
			}
			return len;
		}

		public static int calc(byte[] bytes, int start) {
			if ((bytes[start] & 0x80) == 0x80) {
				int v = 0;

				int e = start + bytes[start] & 0x07;
				while (++start <= e) {
					v <<= 8;
					v |= bytes[start] & 0xFF;
				}

				return v;
			}

			return bytes[start];
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public static BerL read(byte[] bytes, int start) {
			return new BerL(Arrays.copyOfRange(bytes, start,
					start + test(bytes, start)));
		}

		public BerL(byte[] bytes) {
			super(bytes);
			val = calc(bytes, 0);
		}

		public int toInt() {
			return val;
		}
	}

	public final static class BerV extends Iso7816 {
		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public static BerV read(byte[] bytes, int start, int len) {
			return new BerV(Arrays.copyOfRange(bytes, start, start + len));
		}

		public BerV(byte[] bytes) {
			super(bytes);
		}
	}


	public final static class Tag {
		private final IsoDep nfcTag;
		private ID id;

		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public Tag(IsoDep tag) {
			nfcTag = tag;
			id = new ID(tag.getTag().getId());
		}

		public ID getID() {
			return id;
		}




		public Response getBalance(boolean isEP) {
			final byte[] cmd = { (byte) 0x80, // CLA Class
					(byte) 0x5C, // INS Instruction
					(byte) 0x00, // P1 Parameter 1
					(byte) (isEP ? 2 : 1), // P2 Parameter 2
					(byte) 0x04, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readRecord(int sfi, int index) {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB2, // INS Instruction
					(byte) index, // P1 Parameter 1
					(byte) ((sfi << 3) | 0x04), // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readRecord(int sfi) {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB2, // INS Instruction
					(byte) 0x01, // P1 Parameter 1
					(byte) ((sfi << 3) | 0x05), // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}

		public Response readBinary(int sfi) {
			final byte[] cmd = { (byte) 0x00, // CLA Class
					(byte) 0xB0, // INS Instruction
					(byte) (0x00000080 | (sfi & 0x1F)), // P1 Parameter 1
					(byte) 0x00, // P2 Parameter 2
					(byte) 0x00, // Le
			};

			return new Response(transceive(cmd));
		}



		public Response selectByID(byte... name) {
			ByteBuffer buff = ByteBuffer.allocate(name.length + 6);
			buff.put((byte) 0x00) // CLA Class
					.put((byte) 0xA4) // INS Instruction
					.put((byte) 0x00) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) name.length) // Lc
					.put(name).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}

		public Response selectByName(byte... name) {
			ByteBuffer buff = ByteBuffer.allocate(name.length + 6);
			buff.put((byte) 0x00) // CLA Class
					.put((byte) 0xA4) // INS Instruction
					.put((byte) 0x04) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) name.length) // Lc
					.put(name).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public void connect() {
			try {
				nfcTag.connect();
			} catch (Exception e) {
			}
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public void close() {
			try {
				nfcTag.close();
			} catch (Exception e) {
			}
		}

		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public byte[] transceive(final byte[] cmd) {
			try {
				return nfcTag.transceive(cmd);
			} catch (Exception e) {
				return Response.ERROR;
			}
		}
	}

	public static final short SW_NO_ERROR = (short) 0x9000;
}
