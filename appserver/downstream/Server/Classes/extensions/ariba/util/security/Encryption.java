/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/release/security/9.29.1+/ariba/util/security/Encryption.java#6 $

    Responsible: rwong
*/

package ariba.util.security;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.Base64;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.MasterPasswordClient;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.TableUtil;
import ariba.util.i18n.I18NUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Encrption class to encrypt Strings and input streams. Two encryption algroithms are
    provided: DES and DESede (that is TripleDES). For all practical purposes, TripleDES
    is the default algorithm and should be used. DES is retained for backward compatibility
    purposes only. New code should not use DES anymore. <p>

    To support interoperability (Ops encrypts their parameters with master password) and
    also non-shared service (where there is no (well not yet) usage of the master password
    and every thing is encrypted with the secret key), 2 flavors of encryption is used: <p>

    (i) encrypt with the latest version of the secret key: This is what application code should
    do. The preferred method to use is @{link #encryptWithPrefix(java.lang.String)} and
    @{link #decryptWithPrefix(java.lang.String). Some application may need to use
    @{link #encrypt(java.lang.String)} and @{link #decrypt(java.lang.String, java.lang.String)},
    the second parmater being the version used to encrypt the String.

    (ii) encrypt system data using the "master password". Only core platform code (or perhaps
    the configurator and migration code) should be concerned with this. Application code should
    not use this. The method to use is @{link #encryptSystemDataWithPrefix(java.lang.String)}
    and @{link #decryptWithPrefix(java.lang.String)}. <p>

    Other code can use the isJCEEncrypted method to find out if the string is encrypted and
    with what algorithm. Strings encrypted with this @{link #encryptWithPrefix} must be decrypted
    using @{link #decryptWithPrefix}. <p>

    The encrypt methods do not prefix the encrypted data with the prefix. The corresponding
    decrypt methods should be used to decrypt. Their use should be limited, except where
    streams are encrypted/decrypted. In all these cases, information about the algorithm and the
    version used must be provided by the caller if needed.<p>

    @aribaapi ariba
*/
public final class Encryption
{
    public static final String AlgorithmDES     = "DES";
    public static final String AlgorithmDESede  = "DESede";
    public static final String Algorithm3DES    = AlgorithmDESede;
    public static final String AlgorithmAESCBC  = "AES/CBC/PKCS5Padding";
    public static final int BufferByteLimit = 256;

    public static final String KeySystem = "System";
    public static final String KeySecurity = "Security";
    public static final String KeyEncryptionKeys = "EncryptionKeys";
    public static final String KeyEncryptionAlgorithms = "EncryptionAlgorithms";
    public static final String KeyCurrentVersion = "CurrentVersion";
    public static final String ParameterSystemSecurityCurrentVersion =
        KeySystem + "." + KeySecurity + "." + KeyCurrentVersion;
    public static final String ParameterSystemSecurityEncryptionKeys =
        KeySystem + "." + KeySecurity + "." + KeyEncryptionKeys;

    private static final int SpoolBufferSize = 4*1024;

    /**
        This is a reserved special version, initially reserved for
        encrypting 'non-system' data but using the 'master password'.

        @aribaapi ariba
    */
    public static final String VersionZero = "_0";
        // indices into various arrays
    private static final int Index_DESede = 0;
    private static final int Index_DES = 1;
    private static final int Num_Algorithms = 3;
    /**
        Supported algorithms. We really only support TripleDES (i.e. DESede),
        support for DES is for backward compatilibity only. New code should
        never use DES as the encryption algorithm.

        @aribaapi ariba
    */
    public static final String[] SupportedAlgorithms = {AlgorithmDESede,
                                                        AlgorithmDES,
                                                        AlgorithmAESCBC};

    private static final String[] DefaultMasterKeys = {
        "jttwtlhc*_eswnudlr_*FFBB",
        "jttwtlhc",
        "jttwtlhc*_eswnu"};

    /**
        Encryption helpers for system data, one for each supported
        algorithms. The DES one is here for backward compatibility.
    */
    private static EncryptionHelper[] systemEncryptionHelpers =
        new EncryptionHelper[Num_Algorithms];

    /**
        The separator between the algorithm and the version
        @aribaapi ariba
    */
    public static final String AlgorithmVersionSeparator = ":";

    /**
        The latest version of encryption key. This corresponding secret key
        will be used for encryption.
    */
    private static String currentVersion = null;

    /**
        A map whose keys are the version number and whose
        values are EncryptionHelper instances instantiated with the corresponding
        secret keys. This map can be null if we don't have any encryption keys specified.
    */
    private static Map encryptionHelpers = null;

    private static Map initializeEncryptionHelpers (Map inputMap)
    {
        if (encryptionHelpers == null || inputMap != null) {
            synchronized (Encryption.class) {
                if (encryptionHelpers == null || inputMap != null) {
                    Map map = getSecurityParams(inputMap);
                    Map algorithms = null;
                    if (!MapUtil.nullOrEmptyMap(map)) {
                        currentVersion = (String)map.get(KeyCurrentVersion);
                        Log.security.debug("current version of encryption key is: %s",
                                           currentVersion);
                        algorithms = (Map)map.get(KeyEncryptionAlgorithms);
                        map = (Map)map.get(KeyEncryptionKeys);
                    }
                    Log.security.debug("There are %s encryption keys configured",
                                       MapUtil.nullOrEmptyMap(map) ? 0 : map.size());
                    checkConfig(map);
                    encryptionHelpers = initializeHelpers(map, algorithms);
                }
            }
        }
        return encryptionHelpers;
    }

    /**
        Called only by unit tests to reset the internal state. No one
        else should call this method.
        @param map the input map that contains the encryption
        configuration.
    */
    static synchronized void resetCurrentVersionAndHelpers (Map map)
    {
        SystemUtil.checkCalledFromUnitTest(Encryption.class.getName(),
                                           "resetCurrentVersionAndHelpers");

        for (int i=0; i<Num_Algorithms; i++) {
            systemEncryptionHelpers[i] = null;
        }
        /* need to set this to null explicitly because
            initializeEncryptionHelpers will only set currentVersion
            if we configure some encryption helpers. In the case where
            we don't configure any helpers, currentVersion will remain
            unchanged. This is not good because we want it to be
            explicitly set to null in this case.
        */
        currentVersion = null;
        encryptionHelpers = initializeEncryptionHelpers(map);
    }

    /**
        Returns the map which is the value of the (optional)
        System.Security parameter in Parameters.table
        @param inputMap the input map that contains the encryption
        configuration. If null, will get the configuration from Parameters.table
        @return the map which is the value of the (optional)
        System.Security parameter in Parameters.table.  Can be
        <b>null</b>.
    */
    protected static Map getSecurityParams (Map inputMap)
    {
        Map map = null;
        if (inputMap != null) {
            map = inputMap;
        }
        else {
            File file = new File(SystemUtil.getConfigDirectory(), "Parameters.table");

                // can't use Parameters service because this is called
                // before parameters are initialized.
            Log.security.debug("loading %s to fetch security parameters...",
                               file.getAbsolutePath());
            map = TableUtil.loadMap(file, false);
            if (MapUtil.nullOrEmptyMap(map)) {
                    // cannot do an Assert here because the encryption keys file
                    // may not be available when we run unit tests because either
                    // (i) the config dir is not present; or (ii) unit tests (like
                    // perl scripts that call tableedit) is not able to redirect
                    // the config dir to the one they wnat.
                Log.security.debug("Cannot load %s", file.getAbsolutePath());
            }
        }
        if (map != null) {
            map = (Map)map.get(KeySystem);
            if (map != null) {
                map = (Map)map.get(KeySecurity);
            }
        }
        return map;
    }

    private static Map initializeHelpers (Map map, Map algorithms)
    {
        Map helpersMap = null;
        if (!MapUtil.nullOrEmptyMap(map)) {
            List versionList = MapUtil.keysList(map);
            String version = null;
            int numVersions = versionList.size();
            if (numVersions > 0) {
                helpersMap = MapUtil.map();
            }
            try {
                for (int i=0; i<numVersions; i++) {
                    version = (String)versionList.get(i);
                    String encryptedKey = (String)map.get(version);
                    String algorithm = null;
                    if (algorithms != null) {
                        algorithm = (String)algorithms.get(version);
                    }
                    DecryptInfo dInfo = new DecryptInfo(encryptedKey);
                    Assert.that(
                        dInfo.getVersion() == null,
                        "encryption keys for %s is not ecrypted with " +
                        "the master password");
                    Assert.that(isSupportedAlgorithm(dInfo.getAlgorithm()),
                                "unsupported algorithm of %s",
                                dInfo.getAlgorithm());
                    String decrypted = decrypt(dInfo);
                    if (algorithm == null) {
                        algorithm = dInfo.getAlgorithm();
                    }
                    EncryptionHelper helper =
                        GenericEncryptionHelper.newEncryptionHelper(
                            algorithm, decrypted);
                    helpersMap.put(version, helper);
                }
            }
            catch (GeneralSecurityException e) {
                Assert.that(false, "fatal error initializing encryption helper " +
                            "version '%s': %s",
                            version, SystemUtil.stackTrace(e));
            }
            catch (IOException e) {
                Assert.that(false, "fatal error initializing encryption helper " +
                            "version '%s': %s",
                            version, SystemUtil.stackTrace(e));
            }
        }
        return helpersMap;
    }

    /**
        check the configured parameters for consistency. If the input
        map contains some entries, then the current encryption version
        must be contained in this map. If this condition is not met,
        an Assert will result.
        @param map the map the contains the encryption keys and their
        corresponding values, can be <b>null</b>, otherwise, is a map
        of encrtyption versions and their corresponding values.
    */
    private static void checkConfig (Map map)
    {
        Assert.that((MapUtil.nullOrEmptyMap(map) &&
                     StringUtil.nullOrEmptyOrBlankString(currentVersion)) ||
                    (!MapUtil.nullOrEmptyMap(map) &&
                     MapUtil.keysList(map).contains(currentVersion)),
                    "Configuration error: value of %s (%s), is not in '%s' (%s)",
                    ParameterSystemSecurityCurrentVersion,
                    currentVersion,
                    ParameterSystemSecurityEncryptionKeys,
                    map);
    }

    private static int getIndex (String algorithm)
    {
        int index = ArrayUtil.indexOf(SupportedAlgorithms, algorithm);
        Assert.that(index != -1, "Unsupported algorithm: %s", algorithm);
        return index;
    }

    /**
        Returns the current version to be used when encrypting non
        system data. This is the version used when @{link
        #encryptWithPrefix} is called.
        @return the current version to be used when encrypting non
        system data.
        @aribaapi ariba
    */
    public static String getCurrentEncryptVersion ()
    {
        if (currentVersion == null) {
            synchronized (Encryption.class) {
                if (currentVersion == null) {
                    initializeEncryptionHelpers(null);
                }
            }
        }
        return currentVersion;
    }


    /**
        Gets the EncrytionHelper used to encrypt system data.
        @return the EncrytionHelper used to encrypt system data.
        @aribaapi ariba
    */
    public static EncryptionHelper getSystemEncryptionHelper ()
    {
        return getSystemEncryptionHelper(AlgorithmDESede);
    }

    /**
        Gets the EncrytionHelper used to encrypt system data.
        @return the EncrytionHelper used to encrypt system data and a
        specified algorithm
        @param algorithm the encryption algorithm to use, must be a
        supported one.
        @aribaapi ariba
    */
    private static EncryptionHelper getSystemEncryptionHelper (String algorithm)
    {
        int index = getIndex(algorithm);
            // DES needed only for compatibility, in case we strings
            // encrypted with DES. Don't bother to check master
            // password because we don't support DES with master
            // password.
        return getSystemEncryptionHelper(index, algorithm == AlgorithmDES);
    }

    /**
        Gets the EncrytionHelper used to encrypt system data.
        @param index this is the index that specifies the encryption algorithm to use.
        @param ignoreMasterPassword <b>true</b> to bypass master password lookup. This is
        true for DES which we don't support master password usage. Should be <b>false</b> for
        all other encryption algorithms (currently we only have 3DES).
        @return the EncrytionHelper used to encrypt system data.
        @aribaapi ariba
    */
    private static EncryptionHelper getSystemEncryptionHelper
        (int index, boolean ignoreMasterPassword)
    {
            // No need to synchronize. Last one wins. Because of possible race
            // conditions, the returned helper may not be the static systemEncryptionHelper,
            // but that's okay because it's just another helper instance with the same
            // secret key.
        EncryptionHelper helper = systemEncryptionHelpers[index];
        if (helper == null) {
            String mpw = null;
            if (!ignoreMasterPassword) {
                mpw = MasterPasswordClient.getMasterPasswordClient().getMasterPassword();
            }
            if (mpw == null) {
                mpw = DefaultMasterKeys[index];
            }
            helper = GenericEncryptionHelper.newEncryptionHelper(
                SupportedAlgorithms[index], mpw);
            systemEncryptionHelpers[index] = helper;
        }
        return helper;
    }

    /**
        Gets the encrytionHelper for the default algorithm. This method should be
        used to obtain the encrytpion helper for encrypting non-sysem data.
        @return the encryption helper.
        @aribaapi ariba
    */
    public static EncryptionHelper getEncryptionHelper ()
    {
        return getEncryptionHelper(AlgorithmDESede, getCurrentEncryptVersion());
    }
    /**
        Gets the encrytionHelper for a given algorithm. This method should be
        used to obtain the encrytpion helper for encrypting non-sysem data.
        @param algorithm the encryption algorithm name, we only support
        DES (for backward compatibility) and DESede (Triple DES) currently.
        @return the encryption helper.
        @aribaapi ariba
    */
    public static EncryptionHelper getEncryptionHelper (String algorithm)
    {
        return getEncryptionHelper(algorithm, getCurrentEncryptVersion());
    }

    /**
        Gets the encrytionHelper for a given algorithm and a given
        version. This method should be used to get the helper for
        decryption. Use getEncryptionHelpe(String) for encryption.
        @param algorithm the encryption algorithm name, we only support
        DES (for backward compatibility) and DESede (Triple DES) currently.
        @param version the version of secret key to use. This should be the version
        used when the corresponding data is encrypted.
        @return the encryption helper, or null if no such helper can be found.
    */
    private static EncryptionHelper getEncryptionHelper (String algorithm,
                                                         String version)
    {
    	/**
    	 * S. Dey - Ariba, Inc
    	Modification to default the verison to 1 when the verison key looks like

    	?xml version="1.0" encoding="UTF-8"?>
    	<!DOCTYPE cXML SYSTEM

    	**/

    	if(version!=null) {
    	    if(version.contains("?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
    	      version = "1";
    	    }
    	}

        EncryptionHelper helper = getEncryptionHelperNullable(algorithm, version);
        if (helper == null) {
        	Log.security.warning(10669, algorithm, version);
        	throw new RuntimeException("Cannot find secret key for version: " +
                                       version + " and algorighm: " + algorithm);
        }
        return helper;
    }


    private static EncryptionHelper getEncryptionHelperNullable (String algorithm,
                                                         String version)
    {
        if (StringUtil.nullOrEmptyString(version)) {
            return getSystemEncryptionHelper(algorithm);
        }
        Map helpers = initializeEncryptionHelpers(null);
        Log.security.debug("getting encryptionHelper for %s, version %s",
                           algorithm, version);
        if (!VersionZero.equals(version)) {
            Assert.that(helpers != null,
                        "No encryption helpers, please check the value of " +
                        "these parameters in Parameters.table: %s and %s",
                        ParameterSystemSecurityEncryptionKeys,
                        ParameterSystemSecurityCurrentVersion);
        }
        EncryptionHelper helper =
            helpers == null ? null : (EncryptionHelper)helpers.get(version);
            // if we can't find the helper for the special version, we
            // use the system encryption helper.
        if (helper == null && VersionZero.equals(version)) {
             helper = getSystemEncryptionHelper(algorithm);
        }

        return helper;
    }




    /**
        Determines if the specified String is encrypted or not. We
        had old code that uses the old aribaencode tool to encrypt.
        This method determines that.
        @param toDecrypt the encrypted string to decrypt
        @return <b>true</b> if the input string is encrypted with
        this encryption tool. <b>false</b> otherwise. Will return
        <b>false</b> if the input string is null or empty or blank.
        @aribaapi ariba
    */
    public static final boolean isJCEEncrypted (String toDecrypt)
    {
        if (StringUtil.nullOrEmptyOrBlankString(toDecrypt)) {
            return false;
        }
        return toDecrypt.startsWith("{") && (toDecrypt.indexOf("}") >= 2);
    }

    /**
        Determines if the specified String is encrypted with the specified
        algorithm.
        @param toDecrypt the encrypted string to decrypt. Null or blank or empty Strings
        are considered as unencrypted, so false will be returned in this case.
        @param algorithm the encrypting algorithm.
        @return <b>true</b> if the specified String is encrypted with the specified
        algorithm. <b>false</b> otherwise.
        @aribaapi ariba
    */
    public static final boolean isJCEEncrypted (String toDecrypt, String algorithm)
    {
        if (StringUtil.nullOrEmptyOrBlankString(toDecrypt)) {
            return false;
        }
        return toDecrypt.startsWith(algorithmPrefix(algorithm));
    }

    /**
        Encrypts a specified String with the default algorithm (3DES) using the default encoding
        of UTF-8, and prefix it with the algorithm. An example of a prefixed
        encrtyped string is "{DESede}2WUJURNtSkTFa8cXa0Wz9A==", where DESede is the encrypting
        algorithm. Note that the encrypted bytes are Base64 encoded (except for the prefix).
        This and #decryptWithPrefix are the preferred methods to use as supposed to using #decrypt and
        #encrypt directly because the former pair keeps track of the encrypt version, eliminating the
        need for the caller to manage the version.

        @param toEncrypt This is the String to encrypt, must not be null.
        @return the Base64 encoded encrypted String.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final String encryptWithPrefix (String toEncrypt)
      throws GeneralSecurityException, IOException
    {
        return encryptWithPrefix(toEncrypt, AlgorithmDESede);
    }

    /**
        Encrypts a specified String with the default algorithm (3DES) using the default encoding
        of UTF-8 using all key versions and prefix it with the algorithm. An example of a prefixed
        encrtyped string is "{DESede}2WUJURNtSkTFa8cXa0Wz9A==", where DESede is the encrypting
        algorithm. Note that the encrypted bytes are Base64 encoded (except for the prefix).
        This and #decryptWithPrefix are the preferred methods to use as supposed to using #decrypt and
        #encrypt directly because the former pair keeps track of the encrypt version, eliminating the
        need for the caller to manage the version.

        @param toEncrypt This is the String to encrypt, must not be null.
        @return the Base64 encoded encrypted Strings list.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final List encryptWithPrefixUsingAllKeys (String toEncrypt)
      throws GeneralSecurityException, IOException
    {
        String encoding = I18NUtil.EncodingUTF_8;
        String algorithm = AlgorithmDESede;
        byte[] plainBytes = toEncrypt.getBytes(encoding);
        // first take care of 'special' versions.
        List encryptedStrings = getSpecialEncryptedStrings(toEncrypt);
        // get all the encryption helpers and use each one of them to build
        // list of encrypted strings
        Map helpersMap = initializeEncryptionHelpers(null);
        if (!MapUtil.nullOrEmptyMap(helpersMap)) {
            List versions = MapUtil.keysList(helpersMap);
            Iterator it = versions.iterator();
            while (it.hasNext()) {
                String version = (String)it.next();
                EncryptionHelper helper = (EncryptionHelper)helpersMap.get(version);
                byte[] encryptedBytes = encrypt(plainBytes, helper);
                String encrypted = new String(encryptedBytes, encoding);
                encrypted = StringUtil.strcat(
                    algorithmWithVersionPrefix(algorithm, version),
                    encrypted);
                // add this encrypted string to the list
                encryptedStrings.add(encrypted);
            }
        }
        return encryptedStrings;
    }

    /**
        Returns a list of 'special' encrypted strings. This list consists of 2
        encrypted strings: one encrypted with the master password, and one
        encrypted with the special {@link #VersionZero}.
        @param toEncrypt the string to encrypt
        @return the above list
        @throws GeneralSecurityException
        @throws IOException
    */
    private static final List getSpecialEncryptedStrings (String toEncrypt)
      throws GeneralSecurityException, IOException
    {
        List encryptedStrings = ListUtil.list();
        byte[] encryptedBytes =
            encrypt(toEncrypt.getBytes(I18NUtil.EncodingUTF_8),
                getEncryptionHelper(AlgorithmDESede, VersionZero));
        String encrypted = new String(encryptedBytes, I18NUtil.EncodingUTF_8);
        /* add the encrypted string with special VersionZero */
        encryptedStrings.add(
            StringUtil.strcat(
                algorithmWithVersionPrefix(AlgorithmDESede, VersionZero),
                encrypted));
        /* now add the system one. A note of implementation details: strictly
            speaking, the encrypted string (without the prefix) can be different
            for the system and VersionZero versions. In reality, they are
            encrypted using the same encryption helper. Note that for
            compatilibity and migration considerations, the system and the
            version zero verson of encrypted string must be encrytped using the
            same helper. So there is no point invoking the expensive encryption
            code again. As an optimization, we just reuse encrypted. */
        encryptedStrings.add(
            StringUtil.strcat(algorithmPrefix(AlgorithmDESede), encrypted));
        return encryptedStrings;
    }

    /**
        Encrypts a specified String with a specified algorithm using the default encoding
        of UTF-8, and prefix it with the algorithm encrypting it. An example of a prefixed
        encrtyped string is "{DESede:1}2WUJURNtSkTFa8cXa0Wz9A==", where DESede is the encrypting
        algorithm. Note that the encrypted bytes are Base64 encoded (except for the prefix).
        Most developers should use encryptWithPrefix(String) that will automatically uses
        the default encryption algorithm. encryptWithPrefix(String) is the preferred method to use.
        @param toEncrypt This is the String to encrypt, must not be null.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @return the Base64 encoded encrypted String.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #decryptWithPrefix
        @see #encryptWithPrefix(java.lang.String)
        @aribaapi ariba
    */
    public static final String encryptWithPrefix (String toEncrypt,
                                                  String algorithm)
      throws GeneralSecurityException, IOException
    {
        String encrypted = Encryption.encrypt(toEncrypt, algorithm);
        return StringUtil.strcat(
            algorithmWithVersionPrefix(algorithm, getCurrentEncryptVersion()), encrypted);
    }

    /**
        This is a special API to allow encrypting with master password. This is
        requested from ops. No application code should call this method. TripleDES
        is used for the encryption.
        use {@link #encryptWithPrefix) instead.
        @param plainText This is the String to encrypt, must not be null.
        @exception GeneralSecurityException when security related errors occur
        @aribaapi ariba
    */
    public static final String encryptSystemDataWithPrefix (String plainText)
      throws GeneralSecurityException
    {
        return encryptSystemDataWithPrefix(plainText, Encryption.AlgorithmDESede);
    }

    /**
        This is a special API to allow encrypting with master password. This is
        requested from ops. No application code should call this method.
        use {@link #encryptWithPrefix) instead.
        @param plainText This is the String to encrypt, must not be null.
        @param algorithm the algorithm to use
        @exception GeneralSecurityException when security related errors occur
    */
    private static final String encryptSystemDataWithPrefix (String plainText,
                                                             String algorithm)
      throws GeneralSecurityException
    {
        byte[] bytes = encrypt(plainText.getBytes(),
                               getSystemEncryptionHelper(algorithm));
        return StringUtil.strcat(algorithmPrefix(algorithm), new String(bytes));
    }

    /**
        Encrypts a specified String with the TripleDES algorithm
        using the default encoding of UTF-8. The encrypted bytes are further Base64 encoded.
        Most callers should use encryptWithPrefix where possible because then they can
        use {@link #isJCEEncrypted} to find out if the string is encrypted and the algroithm
        used.
        @param plainText This is the String to encrypt, must not be null.
        @return the Base64 encoded encrypted String.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final String encrypt (String plainText)
      throws GeneralSecurityException, IOException
    {
        return encrypt(plainText, AlgorithmDESede);
    }

    /**
        Encrypts a specified String with a specified algorithm using the default encoding
        of UTF-8.
        The encrypted bytes are further Base64 encoded.
        @param plainText This is the String to encrypt, must not be null.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @return the Base64 encoded encrypted String.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final String encrypt (String plainText, String algorithm)
      throws GeneralSecurityException, IOException
    {
        return encrypt(plainText, algorithm, I18NUtil.EncodingUTF_8);
    }

    /**
        Encrypts a specified String with a specified algorithm and encoding.
        The encrypted bytes are further Base64 encoded.
        @param plainText This is the String to encrypt, must not be null.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede".
        @param encoding the encoding to use, must be a valid encding.
        @return the Base64 encoded encrypted String.
        @exception GeneralSecurityException when security related errors occur
        @exception IOException when IO related errors occur
        @see #encryptWithPrefix
    */
    private static final String encrypt (String plainText,
                                         String algorithm,
                                         String encoding)
      throws GeneralSecurityException, IOException
    {
        byte[] encryptedBytes = encrypt(plainText.getBytes(encoding),
                                        algorithm);
        return new String(encryptedBytes, encoding);
    }

    /**
        Encrypts a specified byte array with a specified algorithm.
        The encrypted bytes are further Base64 encoded.
        @param plainBytes This is the byte array to encrypt, must not be null.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede".
        @return the Base64 encoded encrypted bytes.
        @exception GeneralSecurityException when security related errors occur
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final byte[] encrypt (byte[] plainBytes, String algorithm)
      throws GeneralSecurityException
    {
        Assert.that(plainBytes != null, "input byte array must not be null");
        return encrypt(plainBytes, getEncryptionHelper(algorithm));
    }


    /**
        Encrypts the input bytes with the TripleDES algorithm
        using the default encoding of UTF-8.
        The encrypted bytes are further Base64 encoded.
        @param plainBytes This is the String to encrypt, must not be null.
        @return the Base64 encoded encrypted bytes.
        @exception GeneralSecurityException when security related errors occur
        @exception GeneralSecurityException when IO related errors occur
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final byte[] encrypt (byte[] plainBytes)
      throws GeneralSecurityException
    {
        return encrypt(plainBytes, getEncryptionHelper(AlgorithmDESede));
    }

    /**
        Encrypts a specified byte array with a specified encryption helper.
        The encrypted bytes are further Base64 encoded.

        @param plainBytes This is the byte array to encrypt, must not be null.
        @param helper the encryption helper that knows how to encrypt the bytes.
        @return the Base64 encoded encrypted bytes.
        @exception GeneralSecurityException when security related errors occur
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final byte[] encrypt (byte[] plainBytes, EncryptionHelper helper)
      throws GeneralSecurityException
    {
        byte[] encryptedBytes = helper.getDeterministicHelper().encrypt(plainBytes);
        return Base64.encode(encryptedBytes, 0, encryptedBytes.length);
    }

    /**
        Encrypts the input stream into the specified output stream usiing Triple DES. Note that once this method
        is called, the output stream is no longer available for further write operation. The caller
        should close the output stream after this call.
        @param is the input stream whose data is to encrypt, must not be null.
        @param os the output stream where encrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the encryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int encrypt (InputStream is, OutputStream os, boolean flush)
      throws GeneralSecurityException, IOException
    {
        return encrypt(is, os, flush, getEncryptionHelper(AlgorithmDESede));
    }

    /**
        Encrypts the input stream into the specified output stream. Note that once this method
        is called, the output stream is no longer available for further write operation. The caller
        should close the output stream after this call.
        @param is the input stream whose data is to encrypt, must not be null.
        @param os the output stream where encrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede".
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the encryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int encrypt (InputStream is, OutputStream os, boolean flush,
                                     String algorithm)
      throws GeneralSecurityException, IOException
    {
        return encrypt(is, os, flush, getEncryptionHelper(algorithm));
    }

    /**
        Encrypts the input stream into the specified output stream. Note that once this method
        is called, the output stream is no longer available for further write operation. The caller
        should close the output stream after this call.
        @param is the input stream whose data is to encrypt, must not be null.
        @param os the output stream where encrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param helper the encryption helper, must not be null.
        DES or DESede (TripleDES). Legal values are "DES" or "DESede".
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the encryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int encrypt (
        InputStream is,
        OutputStream os,
        boolean flush,
        EncryptionHelper helper)
      throws GeneralSecurityException, IOException
    {
        return helper.encrypt(is, os, flush);
    }

    /**
     *
     * @param is
     * @param os
     * @param flush
     * @param algoritm
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static final int encryptWithPrefix (
        InputStream is,
        OutputStream os,
        boolean flush,
        String algoritm)
      throws GeneralSecurityException, IOException
    {
        EncryptionHelper helper = getEncryptionHelper(algoritm);
        String version = getCurrentEncryptVersion();
        byte[] prefix = algorithmWithVersionPrefix(algoritm, version).getBytes();
        os.write(prefix);
        return helper.encrypt(is, os, flush);
    }

    /**
        Decrypts the specified prefixed encrypted String using the specified algorithm.
        @param toDecrypt the encrypted String. Assumed to have been obtained via
        the encryptWithPrefix method. It is illegal for this string to not contain the prefix.
        @return the decrypted String, note that the returned String does not contain the prefix.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @exception IOException when IO errors occurs during the decryption.
        @see #encryptWithPrefix
        @aribaapi ariba
    */
    public static final String decryptWithPrefix (String toDecrypt)
      throws GeneralSecurityException, IOException
    {
        Log.security.debug("decryptWithPrefix: string to be decrypted is %s",
                           toDecrypt);
        return decrypt(new DecryptInfo(toDecrypt));
    }

        // xxx rwong: can get rid of IOException, but note API change
    private static final String decrypt (DecryptInfo info)
      throws GeneralSecurityException, IOException
    {
        String algorithm = info.getAlgorithm();
        String version = info.getVersion();
        byte[] bytesToDecrypt = info.getEncryptedBytes();
        return new String(Encryption.decrypt(bytesToDecrypt,
                                             getEncryptionHelper(algorithm, version)));
    }

    /**
        Decrypts the specified encrypted String using the default algorithm (DESede).
        @param encryptedText the encrypted String. Assumed to have been obtained via
        the variious encrypt methods.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @exception IOException when IO errors occurs during the decryption.
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final String decrypt (String encryptedText)
      throws GeneralSecurityException, IOException
    {
        return decrypt(encryptedText, AlgorithmDESede, getCurrentEncryptVersion());
    }

    /**
        Decrypts the specified encrypted String using the specified algorithm.
        @param encryptedText the encrypted String. Assumed to have been obtained via
        the variious encrypt methods.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @return the decrypted String.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @exception IOException when IO errors occurs during the decryption.
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final String decrypt (String encryptedText, String algorithm)
      throws GeneralSecurityException, IOException
    {
        return decrypt(encryptedText, algorithm, getCurrentEncryptVersion());
    }

    /**
        Decrypts the specified encrypted String using the specified algorithm and version
        @param encryptedText the encrypted String. Assumed to have been obtained via
        the various encrypt methods.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @param version the version of secret key to use. This should be the version
        used when the corresponding data is encrypted.
        @return the decrypted String.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @exception IOException when IO errors occurs during the decryption.
        @see #decryptWithPrefix
    */
    private static final String decrypt (String encryptedText, String algorithm,
                                         String version)
      throws GeneralSecurityException, IOException
    {
        return decrypt(encryptedText, algorithm, I18NUtil.EncodingUTF_8, version);
    }

    /**
        Decrypts the specified encrypted String using the specified algorithm and
        encoding.
        @param encryptedText the encrypted String. Assumed to have been obtained via
        the variious encrypt methods.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @param encoding the encoding to use.
        @param version the version of secret key to use. This should be the version
        used when the corresponding data is encrypted.
        @return the decrypted String.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @exception IOException when IO errors occurs during the decryption.
        @see #decryptWithPrefix
    */
    private static final String decrypt (String encryptedText,
                                         String algorithm,
                                         String encoding,
                                         String version)
      throws GeneralSecurityException, IOException
    {
        Log.security.debug("decrypting %s, version is %s", encryptedText, version);
        byte[] encryptedTextBytes = encryptedText.getBytes(encoding);
        byte[] plainBytes = decrypt(encryptedTextBytes, algorithm, version);
        return new String(plainBytes, encoding);
    }

    /**
        Decrypts the specified encrypted bytes using the default algorithm (TripleDES)
        @param encryptedBytes64 the encrypted bytes. Assumed to have been obtained via
        the variious encrypt methods.
        @return the decrypted bytes.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final byte[] decrypt (byte[] encryptedBytes64, String version)
      throws GeneralSecurityException
    {
        return decrypt(encryptedBytes64, getEncryptionHelper(AlgorithmDESede, version));
    }

    /**
        Decrypts the specified encrypted bytes using the specified algorithm.
        @param encryptedBytes64 the encrypted bytes. Assumed to have been obtained via
        the various encrypt methods.
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @return the decrypted bytes.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @see #decryptWithPrefix
        @aribaapi ariba
    */
    public static final byte[] decrypt (byte[] encryptedBytes64,
                                        String algorithm,
                                        String version)
      throws GeneralSecurityException
    {
        return decrypt(encryptedBytes64, getEncryptionHelper(algorithm, version));
    }


    /**
        Decrypts the specified encrypted bytes.
        @param encryptedBytes64 the encrypted bytes. Assumed to have been obtained via
        the various encrypt methods.
        @param helper the encryption helper, must not be null.
        @return the decrypted bytes.
        @exception GeneralSecurityException when errors occurs during the decryption.
        @aribaapi ariba
    */
    public static final byte[] decrypt (byte[] encryptedBytes64,
                                        EncryptionHelper helper)
      throws GeneralSecurityException
    {
        byte[] encryptedBytes = Base64.decode(encryptedBytes64, 0,
                                              encryptedBytes64.length);
        return helper.getDeterministicHelper().decrypt(encryptedBytes);
    }

    /**
        Decrypts the given input stream into the specified output stream.

        Note: this algorithm is NOT compatible with the string/byte versions.

        @param is the input stream with encrypted data, must not be null.
        @param os the output stream where decrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param version the version of secret key to use. This should be the version
        used when the corresponding data is encrypted.
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the decryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int decrypt (InputStream is, OutputStream os, boolean flush,
                                     String version)
      throws GeneralSecurityException, IOException
    {
        return decrypt(is, os, flush, getEncryptionHelper(AlgorithmDESede, version));
    }


    /**
        Decrypts the given input stream into the specified output stream.

        Note: this algorithm is NOT compatible with the string versions.

        @param is the input stream with encrypted data, must not be null.
        @param os the output stream where decrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param algorithm the encryption algorithm to use. Currently only support
        DES or DESede (TripleDES). Legal values are "DES" or "DESede"
        @param version the version of secret key to use. This should be the version
        used when the corresponding data is encrypted.
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the decryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int decrypt (InputStream is, OutputStream os, boolean flush,
                                     String algorithm, String version)
      throws GeneralSecurityException, IOException
    {
        return decrypt(is, os, flush, getEncryptionHelper(algorithm, version));
    }

    /**
        Decrypts the given input stream into the specified output stream.

        Note: this algorithm is NOT compatible with the string versions.

        @param is the input stream with encrypted data, must not be null.
        @param os the output stream where decrypted data is to be written, must not be null.
        @param flush if <b>true</b> <b>output</b> is flushed after
        every write call
        @param helper the encryption helper, must not be null.
        @return the number of bytes written to the output stream
        @exception GeneralSecurityException when an exception occurs during the decryption process.
        @exception IOException when an I/O error occurs.
        @aribaapi ariba
    */
    public static final int decrypt (InputStream is, OutputStream os, boolean flush,
                                     EncryptionHelper helper)
      throws GeneralSecurityException, IOException
    {
        return helper.decrypt(is, os, flush);
    }

    /**
        Returns a prefix that specifies the algorithm being used.
        @param algorithm the algorithm used, cannot be <b>null</b>
        @return the prefix that specifies the algorithm being used.
        @aribaapi ariba
    */
    public static final String algorithmPrefix (String algorithm)
    {
        return Fmt.S("{%s}", algorithm);
    }

    /**
        Returns a prefix that specifies the algorithm and the version
        of secret key being used.
        @param algorithm the algorithm used, cannot be <b>null</b>.
        @param version the version of secret key to use. If null or empty
        String, @{link #VersionZero} is used.
        @return the prefix that specifies the algorithm and the version being used.
        @aribaapi ariba
    */
    public static final String algorithmWithVersionPrefix (String algorithm,
                                                           String version)
    {
        if (AlgorithmDES.equals(algorithm)) {
               // we don't do version encryption with DES.
            return algorithmPrefix(algorithm);
        }
        if (StringUtil.nullOrEmptyOrBlankString(version)) {
            version = VersionZero;
        }
        return Fmt.S("{%s%s%s}", algorithm, AlgorithmVersionSeparator, version);
    }

    /**
        Checks if the specified encryption algorithm is supported
        @param algorithm the algorithm to check
        @return boolean indicating if the specified encryption algorithm is supported.
    */
    static boolean isSupportedAlgorithm (String algorithm)
    {
        return ArrayUtil.contains(SupportedAlgorithms, algorithm);
    }

    /**
        Determines if the specified key is a encryption key parameter, and returns
        a boolean to indicate the result.

        @param dottedKey a string in dotted key notation.
        @return <code>true</code> if the specified key is a encryption key parameter
        <code>false</code> otherwise.
        @aribaapi ariba
    */
    public static  boolean isEncryptionKey (String dottedKey)
    {
        return dottedKey.startsWith(ParameterSystemSecurityEncryptionKeys + ".") &&
            dottedKey.charAt(dottedKey.length()-1) != '.';
    }

    /**
        Determines if the give encryption key specifies a reserved version.
        @param dottedKey the encryption key. This key must be an encryption key, that is,
        isEncryptionKey(dottedKey) returns true.
        @return <code>true</code> if it is a reserved version. <code>false</code> otherwise.
        @aribaapi ariba
    */
    public static boolean isReservedVersion (String dottedKey)
    {
        int pos = dottedKey.lastIndexOf('.');
        // note that the position cannot even be the end points.
        Assert.that((pos > 0 && pos < (dottedKey.length() - 1)),
            "invalid input encryption key: %s", dottedKey);
        return VersionZero.equals(dottedKey.substring(pos + 1));
    }


    /**
        Returns a wrapper that encrypts data as it gets written to
        the argument. The encryption happens with the default
        algorithm and key version, which gets added to the beginning
        of the input stream

        Note: this algorithm is NOT compatible with the string versions.

         @param os OutputStream
         @return wrapped output stream which encrypts data before it gets
                 written to the underlying output stream.
    */
    public static OutputStream getEncryptingOutputStream (OutputStream os)
            throws IOException
    {
        try {
            EncryptionHelper h = getEncryptionHelper();
            writeString(os, getCurrentEncryptVersion());
            return h.getEncryptingOutputStream(os);
        }
        catch (java.security.GeneralSecurityException e)
        {
            Assert.fail(e, "Should not have caught this exception");
            return null;
        }
        catch (UnsupportedEncodingException e)
        {
            Assert.fail(e, "Should not have caught this exception");
            return null;
        }
    }

    public static InputStream getEncryptingInputStream (InputStream is)
            throws IOException
    {
        try {
            EncryptionHelper h = getEncryptionHelper();

            return h.getEncryptingInputStream(is, stringBytes(getCurrentEncryptVersion()));
        }
        catch (java.security.GeneralSecurityException e)
        {
            Assert.fail(e, "Should not have caught this exception");
            return null;
        }
    }

    /**
        Returns a wrapper that decrypts data as it gets read from
        the argument. The decryption happens with the
        algorithm and key version contained at the beginning of the input stream

        Note: this algorithm is NOT compatible with the string versions.

         @param is InputStream
         @return wrapped input stream which decrypts data from the
                 underlying input stream
    */
    public static InputStream getDecryptingInputStream (InputStream is)
        throws IOException
    {
        try {
            EncryptionHelper h = getHelperFromStream(is);
            return h.getDecryptingInputStream(is);
        }
        catch (java.security.GeneralSecurityException e)
        {
            Assert.fail(e, "Should not have caught security exception");
            return null;
        }
    }


    private static void writeString (OutputStream os, String s)
            throws IOException, GeneralSecurityException
    {
        os.write(stringBytes(s));
    }

    private static byte[] stringBytes (String s)
            throws GeneralSecurityException
    {
        try {
            if (s == null) {
                return new byte[] { 0 };
            }
            else {
                byte[] content = s.getBytes(I18NUtil.EncodingUTF8);
                byte[] buf = new byte[content.length+1];
                buf[0] = (byte)content.length;
                System.arraycopy(content, 0, buf, 1, content.length);
                return buf;
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new GeneralSecurityException(e);
        }
    }

    /**
     *   Parse the inputstream to determine the new crypt version of the
     *   encrypted string. This version is started from SharedService version of the code
     *   ported back to CD version.
     *
     *   @param is the input stream with encrypted data, must not be null.
     *   @param exceptionIfVersionUnRecognized tells API to throw an exception when the first byte
     *          does not yield the number of bytes to read for reading the version number
     *   @return version of the encrypted string, or null
     *   @exception GeneralSecurityException when an exception occurs during the decryption process.
     *   @exception IOException when an I/O error occurs.
     *   @aribaapi ariba
     */
    private static String readNewVersion (InputStream is, int len, boolean exceptionIfVersionUnRecognized)
        throws IOException, GeneralSecurityException
    {
        if (len < 0) {
            throw new GeneralSecurityException("Could not read header to decrypt");
        }
        if (len == 0) {
            return null;
        }
        byte[] b = new byte[len];
        if (is.read(b) != len) {
        	// caller may just want to know if version can be read
        	// and expects null when it cannot be determined instead of
        	// getting an exception
        	if (exceptionIfVersionUnRecognized) {
                throw new GeneralSecurityException(
                    Fmt.S("Could not read %s header bytes to decrypt", len));
        	}
        	return null;
        }

        return new String(b, I18NUtil.EncodingUTF8);
    }

    /**
     *   Parse the inputstream to determine the crypt version of the
     *   encrypted string.
     *
     *   @param is the input stream with encrypted data, must not be null.
     *   @param exceptionIfVersionUnrecognized tells it to throw an exception when
     *          the expected format of the version is unrecognized
     *   @return version of the encrypted string
     *   @exception GeneralSecurityException when an exception occurs during the decryption process.
     *   @exception IOException when an I/O error occurs.
     *   @aribaapi ariba
     */
    private static String[] readVersion (InputStream is, boolean exceptionIfVersionUnRecognized)
      throws IOException, GeneralSecurityException
    {

        String[] versionstr = new String[2];
        char[] cryptInfo = new char[32];
        int counter = 0;
        int register;
        int start = is.read();
        // new version of encryption is used which does not give the format
        // of {DESDe:1} but just the version number, and we default to use
        // DESDe algorithm
        if (start != '{') {
            versionstr[0] = Encryption.AlgorithmDESede;
            versionstr[1] = readNewVersion(is, start, exceptionIfVersionUnRecognized);
            if (versionstr[1] == null) {
            	return null;
            }
            return versionstr;
        }
        register = is.read();
        while (register != '}') {
            if (counter >= 32) {
            	if (exceptionIfVersionUnRecognized) {
                    Assert.fail("Should have found a terminating '}'");
            	}
            	return null;
            }
            cryptInfo[counter++] = (char)register;
            register = is.read();
        }
        String toDecrypt = String.valueOf(cryptInfo, 0, counter);
        int separator = toDecrypt.indexOf(Encryption.AlgorithmVersionSeparator);
        if (separator < 0) {
            versionstr[0] = toDecrypt.substring(0);
            versionstr[1] = null;
        }
        else {
            versionstr[0] = toDecrypt.substring(0, separator);
            versionstr[1] = toDecrypt.substring(separator + 1);
        }

        return versionstr;
    }

    /**
     *   Parse the inputstream to determine the crypt version of the
     *   encrypted string. Note that the InuptStream passing in will be read
     *   and reset to the beginning, so it must be a resettable inputstream.
     *
     *   @param is the input stream with encrypted data, must not be null, and must be
     *          a markSupported inputstream, as mark and reset will be invoked in this code.
     *
     *   @return true if it has valid encrypted version in the header
     *   @exception GeneralSecurityException when an exception occurs during the decryption process.
     *   @exception IOException when an I/O error occurs.
     *   @aribaapi ariba
     */
    public static final boolean hasEncryptedData (InputStream is)
        throws IOException
    {
    	Assert.that(is.markSupported(),
    			    "InputStream pass in is not a resettable InputSteam!");
        // suppress exception if version cannot be determined from
    	// the header bytes
    	try {
    		is.mark(BufferByteLimit);
            String[] versionstr = readVersion(is, false);
            if (versionstr == null) {
            	return false;
            }
            // in case we got clear text data, the version string may have been
            // read in as version header and did not realize that the data was not
            // encrypted; we need to add a check to make sure the read in version
            // can actaully be mapped to a valid EncryptHelper in order to return
            // true here
            return (getEncryptionHelperNullable(versionstr[0], versionstr[1]) != null);
    	}
    	catch (GeneralSecurityException ex) {
            Log.security.warn("Unexpected GeneralSecurityException recevied", ex);
    		return false;
    	}
    	finally {
    		is.reset();
    	}
    }

    private static EncryptionHelper getHelperFromStream (InputStream is)
            throws GeneralSecurityException, IOException
    {
        EncryptionHelper helper = null;

        // if version is not determined try default system encryption helper
        // and use DESede default algorithm, and a null version
        String[] versionstr = readVersion(is, true);
        if (versionstr == null) {
            versionstr = new String[2];
            versionstr[0] = Encryption.AlgorithmDESede;
            versionstr[1] = null;
        }
    	helper = getEncryptionHelper(versionstr[0], versionstr[1]);

        return helper;
    }
}
