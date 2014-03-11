/**
 * 	Author	: 	Arasan Rajendren
 *  Usage	:	9R1 SSO Implementation
 *  Date	:	02/14/2011
 *
 */

package config.java.custom.sso;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.j2ee.core.AribaServlet;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CATSSORelayServlet extends AribaServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "-1");

		String user = request.getHeader("HTTP_CATCUPID");
		String key = request.getParameter("key");
		String ret = request.getParameter("ret");
		boolean isProduction = Base.getService().isProduction();

		Log.customer.debug(ClassName + " Value of User - %s, Key - %s, Return URL - %s", user, key, ret);

		//To accomodate custom login read CUPID parameter
		//If CUPID parameter exists then use the CUPID as loginid.
		String cupid = null;
		Cookie[] cookies = request.getCookies();
		for(int i=0; i<cookies.length; i++) {
			if(cookies[i].getName().equals("cupid")) {
				cupid = cookies[i].getValue();
			}
		}

		Log.customer.debug(ClassName + " Value of CUPID - %s", cupid);
		if(!StringUtil.nullOrEmptyOrBlankString(cupid) && !isProduction ) {
			user = cupid;
		}

		if (user == null || key == null || ret == null) {
			Log.customer.debug(ClassName + " Required values are null");
			return;
		}

		String keyAndUser = key + user;
		String signedUnEncoded = signIt(keyAndUser);
		String signedEncoded = java.net.URLEncoder.encode(signedUnEncoded, "UTF-8");
		String returnURL = ret + "&user=" + user + "&sig=" + signedEncoded;

		Log.customer.debug(ClassName + " Value of returnURL - %s", returnURL);

		response.sendRedirect(returnURL);
	}

	/**
	 *
	 * @param keyToSign
	 * @return RSA Signed Key
	 */

	private String signIt(String keyToSign) {

		byte[] realSig = null;
		String signedString = null;
		PrivateKey privateKey = null;

		try {
			privateKey = getPrivateKey();
			if(privateKey == null) {
				Log.customer.debug(ClassName + " PrivateKey is null, Exiting!");
				return signedString;
			}
			Signature rsaSignature = Signature.getInstance("SHA1withRSA");
			rsaSignature.initSign(privateKey);
			byte[] bytes = keyToSign.getBytes();
			rsaSignature.update(bytes, 0, bytes.length);
			realSig = rsaSignature.sign();
			signedString = new sun.misc.BASE64Encoder().encode(realSig);
		} catch (NoSuchAlgorithmException e) {
			Log.customer.debug(ClassName + " NoSuchAlgorithmException in signIt - " + e.fillInStackTrace());
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			Log.customer.debug(ClassName + " InvalidKeyException in signIt - " + e.fillInStackTrace());
			e.printStackTrace();
		} catch (SignatureException e) {
			Log.customer.debug(ClassName + " SignatureException in signIt - " + e.fillInStackTrace());
			e.printStackTrace();
		}

		return signedString;
	}

	/**
	 *
	 * @return PrivateKey
	 */

	private PrivateKey getPrivateKey() {

		PrivateKey privateKey = null;
		String passwordAdapterInUse = null;
		String privateKeyFilePath = null;
		File privateKeyFile = null;
		DataInputStream dis = null;
		PKCS8EncodedKeySpec keySpec = null;
		KeyFactory keyFactory = null;

		try {
			passwordAdapterInUse = Base.getService().getParameter(Partition.None, PasswordAdapterInUse);
			privateKeyFilePath = Base.getService().getParameter(null, Fmt.S("System.PasswordAdapters.%s.PrivateKeyFilePath", passwordAdapterInUse));
			privateKeyFile = new File(privateKeyFilePath);
			dis = new DataInputStream(new FileInputStream(privateKeyFile));
			byte[] keyBytes = new byte[(int) privateKeyFile.length()];
			dis.readFully(keyBytes);
			dis.close();
			keySpec = new PKCS8EncodedKeySpec(keyBytes);
			keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(keySpec);
			Log.customer.debug(ClassName + " Value of PrivateKey - %s", privateKey);
		} catch (Exception e) {
			Log.customer.debug(ClassName + " Exception in generating PrivateKey - " + e.fillInStackTrace());
			e.printStackTrace();
		}

		return privateKey;
	}


	public static final String PasswordAdapterInUse = "Application.Authentication.PasswordAdapter";
	public static final String ClassName = CATSSORelayServlet.class.getName();

}

