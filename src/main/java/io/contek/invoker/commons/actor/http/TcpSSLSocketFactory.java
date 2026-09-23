package io.contek.invoker.commons.actor.http;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class TcpSSLSocketFactory extends SSLSocketFactory {

  private static final X509TrustManager DEFAULT_TRUST_MANAGER = createDefaultTrustManager();
  private static final SSLSocketFactory DELEGATE = createDefaultSocketFactory();

  private static SSLSocketFactory createDefaultSocketFactory() {
    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] {DEFAULT_TRUST_MANAGER}, null);
    } catch (NoSuchAlgorithmException e) {
      System.err.println("No such algorithm.");
      throw new RuntimeException(e);
    } catch (KeyManagementException e) {
      throw new RuntimeException(e);
    }
    return sslContext.getSocketFactory();
  }

  public static X509TrustManager getDefaultTrustManager() {
    return DEFAULT_TRUST_MANAGER;
  }

  private static X509TrustManager createDefaultTrustManager() {
    try {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm()
      );
      tmf.init((KeyStore) null);
      return (X509TrustManager) tmf.getTrustManagers()[0];
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      throw new RuntimeException(e);
    }
  }

  private static Socket configure(Socket s) throws SocketException {
    s.setTcpNoDelay(true);      // Disable Nagle
    s.setKeepAlive(true);       // Enable TCP keepalive
    return s;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return DELEGATE.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return DELEGATE.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return configure(DELEGATE.createSocket(s, host, port, autoClose));
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return configure(DELEGATE.createSocket(host, port));
  }

  @Override
  public Socket createSocket(String host, int port,
                               InetAddress local, int localPort) throws IOException {
    return configure(DELEGATE.createSocket(host, port, local, localPort));
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return configure(DELEGATE.createSocket(host, port));
  }

  @Override
  public Socket createSocket(InetAddress host, int port,
                             InetAddress local, int localPort) throws IOException {
    return configure(DELEGATE.createSocket(host, port, local, localPort));
  }
}
