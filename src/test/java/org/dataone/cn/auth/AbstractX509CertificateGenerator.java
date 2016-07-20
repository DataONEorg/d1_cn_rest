/**
 * This work was created by participants in the DataONE project, and is jointly copyrighted by participating
 * institutions in DataONE. For more information on DataONE, see our web site at http://dataone.org.
 *
 * Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * $Id$
 */

package org.dataone.cn.auth;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *
 * @author waltz
 */
public abstract class AbstractX509CertificateGenerator implements X509CertificateGenerator {

    protected static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    // the next two constants could be variables to the method below
    private static final int validityDays = 365;
    private static Logger logger = Logger.getLogger(X509CertificateGenerator.class);
    private static X509Certificate mockCAX509Certificate = null;
    private static KeyPair caKeyPair = null;
    SecureRandom sr = null;

    public AbstractX509CertificateGenerator() {
        sr = new SecureRandom();
        sr.setSeed(System.currentTimeMillis());
        sr.nextInt();
        if ((Security.getProvider(BC) == null) || (Security.getProvider(BC).isEmpty())) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private X509Certificate generateCACert(PublicKey pubKey, PrivateKey privKey) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException, CertIOException {
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, validityDays);
        Date startDate = new Date(System.currentTimeMillis());
        Date expireDate = new Date(now.getTimeInMillis());
        // don't know if this is really needed, but what the hey
        BigInteger dateInteger = BigInteger.valueOf(System.currentTimeMillis());
        BigInteger bigSerialNumber = BigInteger.valueOf(sr.nextLong());
        bigSerialNumber.add(dateInteger);
        X500NameBuilder builder = getMockCANameBuilder();
        X500Name caName = builder.build();
        ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WITHRSAENCRYPTION").setProvider(BC).build(privKey);
//        sigGen.getSignature()
//        X509v1CertificateBuilder certGen1 = new JcaX509v1CertificateBuilder(builder.build(), bigSerialNumber, startDate, expireDate, builder.build(), pubKey);
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(caName, bigSerialNumber, startDate, expireDate, builder.build(), pubKey);
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(pubKey));
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(pubKey));

        X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certBuilder.build(sigGen));

        cert.checkValidity(new Date());

        cert.verify(pubKey);

        // System.out.println(cert);
        if (!cert.getIssuerDN().equals(cert.getSubjectDN())) {
            logger.error("name comparison fails");
        }
        logger.info(cert.getSubjectDN().getName());
        return cert;
    }

    private X509Certificate generateCASelfSignedCertificate(PublicKey publicKey, KeyPair caKeyPair,
            X509Certificate caCert, String subject)
            throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, InvalidKeyException,
            NoSuchProviderException, SignatureException, IOException {
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        X500NameBuilder builder = getMockDNBuilder(subject);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, validityDays);
        Date startDate = new Date(System.currentTimeMillis());
        Date expireDate = new Date(now.getTimeInMillis());
        // don't know if this is really needed, but what the hay
        BigInteger dateInteger = BigInteger.valueOf(System.currentTimeMillis());
        BigInteger bigSerialNumber = BigInteger.valueOf(sr.nextLong());
        bigSerialNumber.add(dateInteger);

        ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WITHRSAENCRYPTION").setProvider(BC).build(caKeyPair.getPrivate());
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                JcaX500NameUtil.getIssuer(caCert),
                bigSerialNumber,
                startDate, expireDate,
                builder.build(),
                publicKey);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature
                | KeyUsage.keyEncipherment));
        ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);

        SubjectPublicKeyInfo subjPubKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKey.getEncoded()));
        SubjectPublicKeyInfo authPubKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(caKeyPair.getPublic().getEncoded()));
        certBuilder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage);

        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(authPubKeyInfo));
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

        X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certBuilder.build(sigGen));
        cert.checkValidity(new Date());
        cert.verify(caCert.getPublicKey());

        return cert;
    }

    private X509Certificate getCACertificate() {

        if (mockCAX509Certificate == null) {
            try {
                caKeyPair = createKeys();

                mockCAX509Certificate = generateCACert(caKeyPair.getPublic(), caKeyPair.getPrivate());
            } catch (NoSuchAlgorithmException ex) {
                logger.fatal(ex, ex);
            } catch (NoSuchProviderException ex) {
                logger.fatal(ex, ex);
            } catch (OperatorCreationException ex) {
                logger.fatal(ex, ex);
            } catch (CertificateException ex) {
                logger.fatal(ex, ex);
            } catch (InvalidKeyException ex) {
                logger.fatal(ex, ex);
            } catch (SignatureException ex) {
                logger.fatal(ex, ex);
            } catch (CertIOException ex) {
                logger.fatal(ex, ex);
            }
        }
        return mockCAX509Certificate;
    }

    @Override
    public X509Certificate getCertificate(String commonName) {
        X509Certificate myX509Certificate = null;
        try {
            X509Certificate caX509Certificate = getCACertificate();
            KeyPair keyPair = createKeys();
            myX509Certificate = generateCASelfSignedCertificate(keyPair.getPublic(), caKeyPair, caX509Certificate, commonName);

        } catch (NoSuchAlgorithmException ex) {
            logger.fatal(ex, ex);
        } catch (NoSuchProviderException ex) {
            logger.fatal(ex, ex);
        } catch (OperatorCreationException ex) {
            logger.fatal(ex, ex);
        } catch (CertificateException ex) {
            logger.fatal(ex, ex);
        } catch (InvalidKeyException ex) {
            logger.fatal(ex, ex);
        } catch (SignatureException ex) {
            logger.fatal(ex, ex);
        } catch (IOException ex) {
            logger.fatal(ex, ex);
        }
        return myX509Certificate;
    }

    abstract protected X500NameBuilder getMockCANameBuilder();

    abstract protected X500NameBuilder getMockDNBuilder(String subject);

    private KeyPair createKeys() throws NoSuchAlgorithmException, NoSuchProviderException {

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, random);
        KeyPair keypair = keyGen.generateKeyPair();

        return keypair;
    }
}
