package linjw.com.grpcdemo;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GrpcDemo";

    private static final int PROT = 55055;
    private static final String NAME = "linjw";
    private static final String HOST = "localhost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServer(PROT);
        startClient(HOST, PROT, NAME);
    }

    private void startServer(int port){
        try {
            Server server = ServerBuilder.forPort(port)
                    .addService(new GreeterImpl())
                    .build()
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
    }

    private void startClient(String host, int port, String name){
        new GrpcTask(host, port, name).execute();
    }

    private class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        public void sayHello(Any request, StreamObserver<HelloReply> responseObserver) {
            try {
                responseObserver.onNext(sayHello(request.unpack(HelloRequest.class)));
                responseObserver.onCompleted();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        private HelloReply sayHello(HelloRequest request) {
            return HelloReply.newBuilder()
                    .setMessage("hello "+ request.getName())
                    .build();
        }
    }

    private class GrpcTask extends AsyncTask<Void, Void, String> {
        private String mHost;
        private String mName;
        private int mPort;
        private ManagedChannel mChannel;

        public GrpcTask(String host, int port, String name) {
            this.mHost = host;
            this.mName = name;
            this.mPort = port;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                mChannel = ManagedChannelBuilder.forAddress(mHost, mPort)
                        .usePlaintext(true)
                        .build();
                GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(mChannel);
                HelloRequest message = HelloRequest.newBuilder().setName(mName).build();
                HelloReply reply = stub.sayHello(Any.pack(message));
                return reply.getMessage();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return "Failed... : " + System.lineSeparator() + sw;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Log.d(TAG, result);
        }
    }
}
