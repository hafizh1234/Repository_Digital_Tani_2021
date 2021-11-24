package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import id.ac.ipb.mobile.digitani.databinding.FragmentDetailLogPengabdianBinding
import id.ac.ipb.mobile.digitani.model.Pengabdian
import id.ac.ipb.mobile.digitani.model.User
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DetailLogPengabdian : DialogFragment() {
    companion object {
        var PENGABDIAN = "pengabdian"
        var USER = "user"
    }

    private var pengabdian: Pengabdian? = null
    private var _binding: FragmentDetailLogPengabdianBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val fragment = parentFragment
        if (fragment is LogPengabdianUntukAdmin || fragment is LogPengabdian) {
            Toast.makeText(requireActivity(), "Melihat Detail", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            var date: Date = Date()
            var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            pengabdian = requireArguments().getParcelable(PENGABDIAN)
            var user = requireArguments().getString(USER)
            var userYangSesuai: User = User(0, "", "", "", "")
            var stringDate = pengabdian?.created_at
            var timeHasil = stringDate!!.split(".") as java.util.ArrayList<String>
            var pengabdianTerbentuk = timeHasil[0]
            var selesai =
                pengabdianTerbentuk.split("T") as java.util.ArrayList<String>

            var result = "${selesai[0]} ${selesai[1]}"
            try {
                date = formatter.parse(result)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            binding.tanggalPengabdian.text = date.toString()
            binding.title.text = pengabdian!!.title
            //Toast.makeText(requireActivity(),"${pengabdian!!.city_id}",Toast.LENGTH_LONG).show()
            binding.penyelesaian.text = pengabdian!!.content
            var isiDaerah = pengabdian!!.city_id!!.split(",")
            //Toast.makeText(requireActivity(),"${isiDaerah.firstOrNull{it==":"}}",Toast.LENGTH_LONG).show()
            var nameDaerah: ArrayList<String> = arrayListOf()

            if(isiDaerah.size==3) {
                var provinsi = isiDaerah[0].split(":")
                var provinsiName = provinsi[1]

                var city = isiDaerah[1].split(":")
                var cityName = city[1]

                var districts = isiDaerah[2].split(":")
                var districtsName = districts[1]

                binding.provinsiNama.text = provinsiName
                binding.kotaNama.text = cityName
                binding.kecamatanName.text = districtsName
            }else{
                binding.provinsiNama.text = ""
                binding.kotaNama.text = ""
                binding.kecamatanName.text = ""
            }
            binding.pakar.text = user
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailLogPengabdianBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return binding.root
    }

}
