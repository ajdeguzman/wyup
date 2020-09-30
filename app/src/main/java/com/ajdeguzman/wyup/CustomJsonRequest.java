package com.ajdeguzman.wyup;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class CustomJsonRequest<T> extends JsonRequest<JSONArray> {

    private JSONObject mRequestObject;
    private Response.Listener<JSONArray> mResponseListener;

    public CustomJsonRequest(int method, String url, JSONObject requestObject, Response.Listener<JSONArray> responseListener,  Response.ErrorListener errorListener) {
        super(method, url, (requestObject == null) ? null : requestObject.toString(), responseListener, errorListener);
        mRequestObject = requestObject;
        mResponseListener = responseListener;
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        return super.parseNetworkError(volleyError);
    }


    @Override
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        return params;
    }
    @Override
    protected void deliverResponse(JSONArray response) {
        mResponseListener.onResponse(response);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            try {
                return Response.success(new JSONArray(json),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (JSONException e) {
                return Response.error(new ParseError(e));
            }
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> params = new HashMap<String, String>();
        String creds = String.format("%s:%s","Af7CBjek5B","5tbM2TbC0H");
        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
        params.put("Authorization", auth);
        return params;
    }
}