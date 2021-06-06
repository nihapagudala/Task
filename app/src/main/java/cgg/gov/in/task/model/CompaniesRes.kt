package cgg.gov.`in`.task.model

data class CompaniesRes(
    var _id: String, var company_id: Int, var company_name: String, var company_description: String,
    var latitude: Double, var longitude: Double, var company_image_url: String, var avg_rating: Int, var distance: Float
)