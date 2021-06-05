package cgg.gov.in.task.network;


import java.util.List;
import java.util.concurrent.TimeUnit;

import cgg.gov.in.task.model.CompaniesRes;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public interface ServiceCalls {

    String VIRTUO_BASE_URL = "http://dev.meetworks.in:9000/companies/";

    class Factory {
        public static ServiceCalls create() {


            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(VIRTUO_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
            return retrofit.create(ServiceCalls.class);
        }

    }

    @GET("getallcompanies/10/20")
    Call<List<CompaniesRes>> getAllCompanies();

}



