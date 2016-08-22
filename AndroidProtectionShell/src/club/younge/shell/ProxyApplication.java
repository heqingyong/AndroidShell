package club.younge.shell;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import club.younge.jni.Security;
import club.younge.util.HexUtil;
import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {
	@SuppressWarnings("unused")
	private static final String appkey = "APPLICATION_CLASS_NAME";
	private String apkFileName;
	private String odexPath;
	private String libPath;
	static {        // ���ض�̬��
        System.loadLibrary("Security");
    }


	// ����context ��ֵ
	@SuppressWarnings("rawtypes")
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		try {
			// ���������ļ���payload_odex��payload_lib ˽�еģ���д���ļ�Ŀ¼
			File odex = this.getDir("payload_odex", MODE_PRIVATE);
			File libs = this.getDir("payload_lib", MODE_PRIVATE);
			odexPath = odex.getAbsolutePath();
			libPath = libs.getAbsolutePath();
			apkFileName = odex.getAbsolutePath() + "/AndroidProtectionDome.apk";
			File dexFile = new File(apkFileName);
			if (!dexFile.exists()) {
				dexFile.createNewFile(); // ��payload_odex�ļ����ڣ�����payload.apk
				// ��ȡ����classes.dex�ļ�
				byte[] dexdata = this.readDexFileFromApk();
				// �������Ǻ��apk�ļ������ڶ�̬����
				this.splitPayLoadFromDex(dexdata);
			}
			// ���ö�̬���ػ���
			Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread",
					"currentActivityThread", new Class[] {}, new Object[] {});// ��ȡ���̶߳���
																				// http://blog.csdn.net/myarrow/article/details/14223493
			String packageName = this.getPackageName();// ��ǰapk�İ���
			// �������䲻��̫���
			Map mPackages = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
					"mPackages");
			WeakReference wr = (WeakReference) mPackages.get(packageName);
			// �������ӿ�apk��DexClassLoader���� ����apk�ڵ���ͱ��ش��루c/c++���룩
			// ���������������dex���ص��������
			DexClassLoader dLoader = new DexClassLoader(apkFileName, odexPath, libPath,
					(ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader"));
			// base.getClassLoader(); �ǲ��Ǿ͵�ͬ�� (ClassLoader)
			// RefInvoke.getFieldOjbect()? �п���֤��//?
			// �ѵ�ǰ���̵�DexClassLoader ���ó��˱��ӿ�apk��DexClassLoader
			// ----�е�c++�н��̻�������˼~~
			RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onCreate() {
		{
			// ���ԴӦ��������Appliction�������滻ΪԴӦ��Applicaiton���Ա㲻Ӱ��Դ�����߼���
			String appClassName = null;
			// ��ȡxml�ļ������õı��ӿ�apk��Applicaiton
			try {
				ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(),
						PackageManager.GET_META_DATA);
				Bundle bundle = ai.metaData;
				if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
					appClassName = bundle.getString("APPLICATION_CLASS_NAME");// className
																				// ��������xml�ļ��еġ�
					// appClassName="org.xiangbalao.domes.DemoApplication";
				} else {

					return;
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			// ��ֵ�Ļ����ø�Applicaiton
			Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread",
					"currentActivityThread", new Class[] {}, new Object[] {});
			Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
					"mBoundApplication");
			Object loadedApkInfo = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication,
					"info");
			// �ѵ�ǰ���̵�mApplication ���ó���null
			RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", loadedApkInfo, null);
			Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
					"mInitialApplication");
			// http://www.codeceo.com/article/android-context.html
			ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke
					.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
			mAllApplications.remove(oldApplication);// ɾ��oldApplication

			ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.LoadedApk",
					loadedApkInfo, "mApplicationInfo");
			ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke
					.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
			appinfo_In_LoadedApk.className = appClassName;
			appinfo_In_AppBindData.className = appClassName;
			Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication",
					loadedApkInfo, new Class[] { boolean.class, Instrumentation.class }, new Object[] { false, null });// ִ��
																														// makeApplication��false,null��
			RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);

			// 5.0 ����ϵͳ android.util.ArrayMap cannot be cast to
			// java.util.HashMap, �Ѿ�����
			Map mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
					"mProviderMap");
			// Iterator it = mProviderMap.values().iterator();
			Iterator it = mProviderMap.entrySet().iterator();
			int i = 0;
			while (it.hasNext()) {
				//Object providerClientRecord = it.next();
				Map.Entry entry = (Map.Entry)it.next();
				Object providerClientRecord = entry.getValue();
				i ++;
				Log.e("younge", i + "");
				try {
					Object localProvider = RefInvoke.getFieldOjbect("android.app.ActivityThread$ProviderClientRecord",
							providerClientRecord, "mLocalProvider");
					RefInvoke.setFieldOjbect("android.content.ContentProvider", "mContext", localProvider, app);
					break;
				} catch (Exception e) {
					Log.e("younge", e.toString());
				}
			}

			app.onCreate();
		}
	}

	/**
	 * �ͷű��ӿǵ�apk�ļ���so�ļ�
	 * 
	 * @param data
	 * @throws IOException
	 */
	private void splitPayLoadFromDex(byte[] data) throws IOException {
		byte[] apkdata = data;
		int ablen = apkdata.length;
		// ȡ���ӿ�apk�ĳ��� ����ĳ���ȡֵ����Ӧ�ӿ�ʱ���ȵĸ�ֵ��������Щ��
		byte[] dexlen = new byte[4];
		System.arraycopy(apkdata, ablen - 4, dexlen, 0, 4);
		ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
		DataInputStream in = new DataInputStream(bais);
		int readInt = in.readInt();
		System.out.println(Integer.toHexString(readInt));
		byte[] newdex = new byte[readInt];
		// �ѱ��ӿ�apk���ݿ�����newdex��
		System.arraycopy(apkdata, ablen - 4 - readInt, newdex, 0, readInt);
		// ����Ӧ�ü��϶���apk�Ľ��ܲ��������ӿ��Ǽ��ܴ���Ļ�
		
		Security security = new Security();
		Log.e("younge", "decrypt before:" + HexUtil.toHex(newdex));
		try {
			newdex = security.decrypt(newdex);
			Log.e("younge", "decrypt afer" + HexUtil.toHex(newdex));
		} catch (Exception e) {
			Log.e("younge", "decrypt:" + e.toString());
		}
		Log.e("younge", "add result:" + security.add(2, 3));
		security.destroy();
		
		//newdex = decrypt(newdex);
		// д��apk�ļ�
		File file = new File(apkFileName);
		try {
			FileOutputStream localFileOutputStream = new FileOutputStream(file);
			localFileOutputStream.write(newdex);
			localFileOutputStream.close();

		} catch (IOException localIOException) {
			throw new RuntimeException(localIOException);
		}

		// �������ӿǵ�apk�ļ�
		ZipInputStream localZipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
		while (true) {
			ZipEntry localZipEntry = localZipInputStream.getNextEntry();// ���˽�����Ƿ�Ҳ������Ŀ¼��������Ӧ���Ǳ�����
			if (localZipEntry == null) {
				localZipInputStream.close();
				break;
			}
			// ȡ�����ӿ�apk�õ���so�ļ����ŵ� libPath�У�data/data/����/payload_lib)
			String name = localZipEntry.getName();
			if (name.startsWith("lib/") && name.endsWith(".so")) {
				File storeFile = new File(libPath + "/" + name.substring(name.lastIndexOf('/')));
				storeFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(storeFile);
				byte[] arrayOfByte = new byte[1024];
				while (true) {
					int i = localZipInputStream.read(arrayOfByte);
					if (i == -1)
						break;
					fos.write(arrayOfByte, 0, i);
				}
				fos.flush();
				fos.close();
			}
			localZipInputStream.closeEntry();
		}
		localZipInputStream.close();

	}

	/**
	 * ��apk�������ȡdex�ļ����ݣ�byte��
	 * 
	 * @return
	 * @throws IOException
	 */
	private byte[] readDexFileFromApk() throws IOException {
		ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
		ZipInputStream localZipInputStream = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(this.getApplicationInfo().sourceDir)));
		while (true) {
			ZipEntry localZipEntry = localZipInputStream.getNextEntry();
			if (localZipEntry == null) {
				localZipInputStream.close();
				break;
			}
			if (localZipEntry.getName().equals("classes.dex")) {
				byte[] arrayOfByte = new byte[1024];
				while (true) {
					int i = localZipInputStream.read(arrayOfByte);
					if (i == -1)
						break;
					dexByteArrayOutputStream.write(arrayOfByte, 0, i);
				}
			}
			localZipInputStream.closeEntry();
		}
		localZipInputStream.close();
		return dexByteArrayOutputStream.toByteArray();
	}

	//ֱ�ӷ������ݣ����߿�������Լ����ܷ���
	@SuppressWarnings("unused")
	private byte[] decrypt(byte[] data) {
		// ģ�ƽ�������
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (data[i] ^ 42);
		}
		return data;
	}
}
