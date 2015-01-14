package com.android.sensorhandle;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class plotDynamic extends View {

	public String title = "default";
	private Paint paint;
	private ArrayList<String> xvalues = new ArrayList<String>();
	private ArrayList<String> yvalues = new ArrayList<String>();
	private float maxy,miny;
	private int xIncrement = 1, initiate = 0, nX;
	
	public plotDynamic(Context context) {
		super(context);
		this.yvalues.add("0");
		paint = new Paint();
		
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		float canvasHeight = getHeight();
		float canvasWidth = getWidth();
		
		if (initiate == 0){
			xvalues.add(""+0.1*canvasWidth);
			nX = (int) (0.9*canvasWidth/xIncrement);
			initiate = 1;
		}
		
		float[] y = new float[yvalues.size()];
		y = getFloat(yvalues);
		maxy=getMax(y);
		miny=getMin(y);
		
		int[] yvaluesInPixels = toPixel(canvasHeight, miny, maxy, y);
		
		canvas.drawARGB(255, 255, 255, 255);

		paint.setStrokeWidth(2);
		paint.setColor(Color.BLACK);
		paint.setTextSize(20.0f);
		canvas.drawLine(0,canvasHeight/2,canvasWidth,canvasHeight/2,paint);
		canvas.drawLine((float)0.1*canvasWidth,0,(float)0.1*canvasWidth,canvasHeight,paint);
		canvas.drawText(title, (float)0.7*canvasWidth,(float)0.1*canvasHeight, paint);
		
		paint.setColor(Color.RED);
		
		if (xvalues.size()==1){
			canvas.drawCircle(fromObjectToFloat(xvalues.get(0)),canvasHeight-yvaluesInPixels[0],2,paint);
		}
		else {
			for (int i = 0; i < xvalues.size()-1; i++) {
				canvas.drawCircle(fromObjectToFloat(xvalues.get(i)),canvasHeight-yvaluesInPixels[i],2,paint);
				canvas.drawCircle(fromObjectToFloat(xvalues.get(i+1)),canvasHeight-yvaluesInPixels[i+1],2,paint);
				canvas.drawLine(fromObjectToFloat(xvalues.get(i)),canvasHeight-yvaluesInPixels[i],fromObjectToFloat(xvalues.get(i+1)),canvasHeight-yvaluesInPixels[i+1],paint);
			}
		}
		
		
	}
	
	private Float fromObjectToFloat(Object o){
		String tempS = o.toString();
		Float f = new Float(tempS);
		return f;
	}
	
	public void addData(float s){
		yvalues.add(""+s);
		
		if (yvalues.size()>nX){
			yvalues.remove(0);
		}
		else {
			float tt = fromObjectToFloat(xvalues.get(xvalues.size()-1))+xIncrement;
			xvalues.add(""+tt);
		}
			
	}
	
	public void clearData(){
		Object temp = xvalues.get(0);
		xvalues.clear();
		xvalues.add(""+temp);
		temp = yvalues.get(0);
		yvalues.clear();
		yvalues.add(""+temp);
	}
	
	private float[] getFloat(ArrayList<String> value) {
		
		float[] v = new float[value.size()];
		
		Object []tempO = value.toArray();
		String[] temp = Arrays.copyOf(tempO, tempO.length, String[].class);
		
		for (int i=0;i<value.size();i++){
			Float f = new Float(temp[i]);
			v[i]=f.floatValue();
		}
		
		return (v);
	}
	
	private int[] toPixel(float pixels, float min, float max, float[] value) {
		
		double[] p = new double[value.length];
		int[] pint = new int[value.length];
		
		for (int i = 0; i < value.length; i++) {
			if(value[i]>0){
				p[i] = pixels/2+((value[i])/(max))*.9*pixels/2;
				pint[i] = (int)p[i];
			}
			else if(value[i]<0){
				p[i] = pixels/2-((value[i])/(min))*.9*pixels/2;
				pint[i] = (int)p[i];
			}
			else {
				pint[i] = (int)pixels/2;
			}
			
		}
		
		return (pint);
	}

	private float getMax(float[] v) {
		float largest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] > largest)
				largest = v[i];
		return largest;
	}

	private float getMin(float[] v) {
		float smallest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] < smallest)
				smallest = v[i];
		return smallest;
	}

}
