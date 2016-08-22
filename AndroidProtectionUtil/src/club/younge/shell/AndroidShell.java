package club.younge.shell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

/**
 *   1������Դ����APK�ļ�Ϊ�������
     2���ѽ������д���ǳ���Dex�ļ�ĩβ�������ļ�β����ӽ�����ݵĴ�С��
     3���޸Ľ�ǳ���DEXͷ��checksum��signature ��file_sizeͷ��Ϣ��
 * @author Younge - heqingyong101@163.com
 *
 */
public class AndroidShell {
	/**
	 * ��Ҫ�ӿǵĳ���(Դ����APK�ļ�)
	 */
	private static String apkPath = "g:/AndroidProtectionDemo.apk";
	/**
	 * ��ǳ���dex
	 */
	private static String shellDexPath = "g:/unshell.dex";
	/**
	 * �ӿǺ��dex
	 */
	private static String newDexFile = "g:/classes.dex";

	public static void main(String[] args) {

		// �������Ĳ����Ƿ���ȷ
		checkArgs(args);

		try {
			File payloadSrcFile = new File(apkPath);
			File unShellDexFile = new File(shellDexPath);
			if (!payloadSrcFile.exists()) {
				System.out.println("APK������");
				return;
			}

			if (!unShellDexFile.exists()) {
				System.out.println("�ӿǳ����dex������");
				return;
			}

			//�������, ΪAPKԴ�ļ�
			byte[] payloadArray = encrpt(readFileBytes(payloadSrcFile));// �Զ�������ʽ����apk�������м��ܴ���

			System.out.println("APK�ĳ��ȣ�" + payloadArray.length);

			byte[] unShellDexArray = readFileBytes(unShellDexFile);// �Զ�������ʽ����dex

			System.out.println("Dex�ĳ��ȣ�" + unShellDexArray.length);
			int payloadLen = payloadArray.length;
			int unShellDexLen = unShellDexArray.length;
			int totalLen = payloadLen + unShellDexLen + 4;// ���4�ֽ��Ǵ�ų��ȵġ�
			byte[] newdex = new byte[totalLen]; // �������µĳ���
			System.out.println("�ӿǺ�Dex�ĳ���" + newdex.length);

			// ��ӽ�Ǵ���
			System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);// �ȿ���dex����
			// ��Ӽ��ܺ�Ľ������
			System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);// ����dex���ݺ��濽��apk������
			// ��ӽ�����ݳ���
			System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen - 4, 4);// ���4Ϊ����
			// �޸�DEX file size�ļ�ͷ
			fixFileSizeHeader(newdex);
			// �޸�DEX SHA1 �ļ�ͷ
			fixSHA1Header(newdex);
			// �޸�DEX CheckSum�ļ�ͷ
			fixCheckSumHeader(newdex);
			// ������д�� newDexFile
			File file = new File(newDexFile);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileOutputStream localFileOutputStream = new FileOutputStream(
					newDexFile);
			localFileOutputStream.write(newdex);
			localFileOutputStream.flush();
			localFileOutputStream.close();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	/**
	 * �������Ĳ���
	 * 
	 * @param args
	 */
	private static void checkArgs(String[] args) {
		if (args.length != 3) {
			System.out.println("������ҪAPK��·��������  D:/AndroidShellDome.apk");

			BufferedReader strin = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				apkPath = strin.readLine();
				System.out.println("APKĿ¼��" + apkPath);
				System.out.println("������ǵ�dex¼�������� D:/unshell.dex");
				strin = new BufferedReader(new InputStreamReader(System.in));
				shellDexPath = strin.readLine();
				System.out.println("�ǵ�dex��¼����" + shellDexPath);

				System.out.println("������ӿǺ��dex·�����ļ���:���� D:/classes.dex");

				strin = new BufferedReader(new InputStreamReader(System.in));
				newDexFile = strin.readLine();
				System.out.println("�ӿǺ��dex·�����ļ���:" + newDexFile);

			} catch (IOException e) {

				e.printStackTrace();
			}

		} else {
			apkPath = args[0];
			System.out.println("APK¼����" + args[0]);

			shellDexPath = args[1];
			System.out.println("dex¼����" + args[1]);
			newDexFile = args[2];
			System.out.println("���ɵ�dex��" + args[2]);
		}
	}

	/**
	 * ֱ�ӷ������ݣ����߿�������Լ����ܷ���
	 * TODO �˴����ܷ�������ʵ��������ѡ��һ�ּ������ܺõĶԳƼ��ܷ�������Ӧ��׿������JNI��ʵ��
	 * @param srcdata
	 * @return
	 */

	private static byte[] encrpt(byte[] srcdata) {

		// ģ�Ƽ�������
		for (int i = 0; i < srcdata.length; i++) {
			srcdata[i] = (byte) (srcdata[i] ^ 42);
		}

		return srcdata;
	}

	/**
	 * �޸�dexͷ��CheckSum У����
	 * 
	 * @param dexBytes
	 */
	private static void fixCheckSumHeader(byte[] dexBytes) {
		Adler32 adler = new Adler32();
		adler.update(dexBytes, 12, dexBytes.length - 12);// ��12���ļ�ĩβ����У����
		long value = adler.getValue();
		int va = (int) value;
		byte[] newcs = intToByte(va);
		// ��λ��ǰ����λ��ǰ������
		byte[] recs = new byte[4];
		for (int i = 0; i < 4; i++) {
			recs[i] = newcs[newcs.length - 1 - i];

		}
		System.arraycopy(recs, 0, dexBytes, 8, 4);// Ч���븳ֵ��8-11��
		System.out.println("�������ֽ������鳤�ȣ�" + newcs.length);
		System.out.println();
	}

	/**
	 * int תbyte[]
	 * 
	 * @param number
	 * @return
	 */
	public static byte[] intToByte(int number) {
		byte[] b = new byte[4];
		for (int i = 3; i >= 0; i--) {
			b[i] = (byte) (number % 256);
			number >>= 8;
		}
		return b;
	}

	/**
	 * �޸�dexͷ sha1ֵ
	 * 
	 * @param dexBytes
	 * @throws NoSuchAlgorithmException
	 */
	private static void fixSHA1Header(byte[] dexBytes)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(dexBytes, 32, dexBytes.length - 32);// ��32Ϊ����������sha--1
		byte[] newdt = md.digest();
		System.arraycopy(newdt, 0, dexBytes, 12, 20);// �޸�sha-1ֵ��12-31��
		// ���sha-1ֵ�����п���
		String hexstr = "";
		for (int i = 0; i < newdt.length; i++) {
			hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
					.substring(1);
		}
		System.out.println("sha-1 ֵ��" + hexstr);

	}

	/**
	 * �޸�dexͷ file_sizeֵ
	 * 
	 * @param dexBytes
	 */
	private static void fixFileSizeHeader(byte[] dexBytes) {
		// ���ļ�����
		byte[] newfs = intToByte(dexBytes.length);

		byte[] refs = new byte[4];
		// ��λ�ں󣬵�λ��ǰ������
		for (int i = 0; i < 4; i++) {
			refs[i] = newfs[newfs.length - 1 - i];

		}
		System.arraycopy(refs, 0, dexBytes, 32, 4);// �޸ģ�32-35��
		System.out.println();
	}

	/**
	 * �Զ����ƶ����ļ�����
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	private static byte[] readFileBytes(File file) throws IOException {
		byte[] arrayOfByte = new byte[1024];
		ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
		FileInputStream fis = new FileInputStream(file);
		while (true) {
			int i = fis.read(arrayOfByte);
			if (i != -1) {
				localByteArrayOutputStream.write(arrayOfByte, 0, i);
			} else {
				return localByteArrayOutputStream.toByteArray();
			}
		}
	}
}
