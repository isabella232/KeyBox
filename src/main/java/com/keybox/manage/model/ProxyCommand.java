package com.keybox.manage.model;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
Inspired by http://stackoverflow.com/questions/21567031/how-to-use-jsch-with-proxycommands-for-portforwarding
Makes it so you can use a proxy locally
**/
public class ProxyCommand implements Proxy {

    private static Logger log = LoggerFactory.getLogger(ProxyCommand.class);
        private String command;
        private Process p = null;
        private InputStream in = null;
        private OutputStream out = null;
        public ProxyCommand(String command){
            this.command = command;
        }
        public void connect(SocketFactory socket_factory, String host, int port, int timeout) throws Exception {
            String _command = command.replace("%h", host);
            _command = _command.replace("%p", Integer.toString(port));
            p = Runtime.getRuntime().exec(_command);
            in = p.getInputStream();
            out = p.getOutputStream();
        }
        public Socket getSocket() { return null; }
        public InputStream getInputStream() { return in; }
        public OutputStream getOutputStream() { return out; }
        public void close() {
            try{
                if(p!=null){
                    p.getErrorStream().close();
                    p.getOutputStream().close();
                    p.getInputStream().close();
                    p.destroy();
                    p=null;
                }
            }
            catch(IOException ex){
                log.error(ex.toString(), ex);
            }
        }
}
