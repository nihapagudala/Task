package cgg.gov.`in`.task.`interface`

import cgg.gov.`in`.task.model.CompaniesRes

interface ServiceInterface {
    fun getCompanies(attendanceResponse: List<CompaniesRes>?)
}