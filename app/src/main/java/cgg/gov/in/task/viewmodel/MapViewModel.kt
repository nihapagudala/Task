package cgg.gov.`in`.task.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.`interface`.ServiceInterface
import cgg.gov.`in`.task.error_handler.ErrorHandlerInterface
import cgg.gov.`in`.task.model.CompaniesRes
import cgg.gov.`in`.task.network.ServiceCalls
import cgg.gov.`in`.task.ui.MapsActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapViewModel : AndroidViewModel {


    private var attendanceResMutableLiveData: MutableLiveData<List<CompaniesRes>>? = null
    private var attendanceInterface: ServiceInterface? = null
    private var context: Context? = null
    private var errorHandlerInterface: ErrorHandlerInterface? = null

    fun MapViewModel() {
    }

    constructor(context: MapsActivity, application: Application?) : super(application!!) {
        this.context = context
        attendanceResMutableLiveData = MutableLiveData<List<CompaniesRes>>()
        errorHandlerInterface = context as ErrorHandlerInterface?
        attendanceInterface = context as ServiceInterface?
    }

    fun getServiceResponse(): LiveData<List<CompaniesRes>?>? {
        if (attendanceResMutableLiveData != null) {
            getServiceResponseCall()
        }
        return attendanceResMutableLiveData
    }

    private fun getServiceResponseCall() {
        val virtuoService: ServiceCalls = ServiceCalls.Factory.create()

        virtuoService.allCompanies.enqueue(object : Callback<List<CompaniesRes>> {
            override fun onResponse(
                call: Call<List<CompaniesRes>?>,
                response: Response<List<CompaniesRes>?>
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    attendanceInterface?.getCompanies(response.body())
                } else {
                    errorHandlerInterface!!.handleError(
                        context!!.getString(R.string.server_not),
                        context
                    )
                }
            }

            override fun onFailure(call: Call<List<CompaniesRes>?>, t: Throwable) {
                errorHandlerInterface!!.handleError(t, context)
            }
        })
    }
}



