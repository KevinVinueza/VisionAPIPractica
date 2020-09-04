package com.example.visionapipractica;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Landmark;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Vision vision;
    ImageView imagen;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(),  null);
        visionBuilder.setVisionRequestInitializer(new
                VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        vision = visionBuilder.build();
    }
    public Image getImageToProcess(){
        imagen = (ImageView)findViewById(R.id.imgImgToProcess);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        bitmap = scaleBitmapDown(bitmap, 1200);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);

        byte[] imageInByte = stream.toByteArray();

        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }

    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);

        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));


        BatchAnnotateImagesRequest batchRequest =  new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }


    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION",
                        getImageToProcess());
                try {

                    Vision.Images.Annotate  annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response  = annotateRequest.execute();


                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();


                    //final StringBuilder message = new StringBuilder("Se ha encontrado los siguientes Objetos:\n\n");
                    // final TextAnnotation text = response.getResponses().get(0).getFullTextAnnotation();
                    /*List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels)
                               message.append(String.format(Locale.US, "%.2f: %s\n",
                                       label.getScore()*100, label.getDescription()));
                    } else {
                        message.append("No hay ningún Objeto");
                    }*/


                    int numberOfFaces = faces.size();
                    String likelihoods = "";

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable =true;
                    Bitmap mybit= BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.familia,options);
                    Paint dib = new Paint();
                    dib.setStrokeWidth(5);
                    dib.setColor(Color.RED);
                    dib.setStyle(Paint.Style.STROKE);

                    Bitmap bt = Bitmap.createBitmap(mybit.getWidth(), mybit.getHeight(), Bitmap.Config.RGB_565);
                    final Canvas dcanvas = new Canvas(bt);
                    dcanvas.drawBitmap(mybit, 0, 0, null);

                    for(int i=0; i<numberOfFaces; i++){
                        likelihoods += "\n Rostro " + i + "  "  + faces.get(i).getJoyLikelihood();
                       List<Landmark> puntos = faces.get(i).getLandmarks();
                        for (int j=0; j< puntos.size()-2; j++){
                        float x = faces.get(i).getLandmarks().get(j).getPosition().getX();
                        float y = faces.get(i).getLandmarks().get(j).getPosition().getY();
                            float x1 = faces.get(i).getLandmarks().get(j+1).getPosition().getX();
                            float y1 = faces.get(i).getLandmarks().get(j+1).getPosition().getY();
                            dcanvas.drawRoundRect(new RectF(x,y,x1,y1),2,2,dib);

                        }


                    }

                    imagen.setImageDrawable(new BitmapDrawable(getResources(), bt));


                    final String message =   "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.txtResult);
                            //imageDetail.setText(text.getText());
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}