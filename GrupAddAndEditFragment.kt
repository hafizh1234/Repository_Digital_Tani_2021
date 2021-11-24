package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.adapter.AdapterGroupAddAndEdit
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentGrupAddAndEditBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.RuangChat
import id.ac.ipb.mobile.digitani.response.ResponseChatRoom
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GrupAddAndEditFragment : Fragment() {
    private lateinit var apiService: ApiInterface
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AdapterGroupAddAndEdit
    private var refresh:String?=null
    private var _binding: FragmentGrupAddAndEditBinding? = null
    private lateinit var arrayListRoom: ArrayList<RuangChat>
    private val binding get() = _binding!!
    companion object{
        var Refresh="Refresh"
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AdapterGroupAddAndEdit(requireActivity())
        sessionManager=SessionManager(requireActivity())
        binding.rvGroupAddAndEdit.layoutManager = LinearLayoutManager(requireActivity())
        arrayListRoom = arrayListOf()
        binding.progressbarGroupAdd.visibility=View.VISIBLE
        if(arguments!=null){
            refresh= arguments?.getString(Refresh)
            if(refresh=="refresh"){
                getAllRoomsGroup()
            }
        }else{
            getAllRoomsGroup()
        }
        binding.rvGroupAddAndEdit.adapter = adapter
        var mBundle = Bundle()
        binding.fabTambahGrup.setOnClickListener {
            var fragment = GroupAddAndEditImplementation()
            mBundle.putString(GroupAddAndEditImplementation.ADD_OR_EDIT, "ADD")
            fragment.arguments = mBundle
            var transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_host_fragment, fragment)
            transaction.commit()
        }
        adapter.setOnItemClickCallback(object : AdapterGroupAddAndEdit.OnItemClickCallback {
            override fun onItemClicked(ruangChat: RuangChat) {
                var fragment = GroupAddAndEditImplementation()
                var mBundle = Bundle()
                mBundle.putParcelable(GroupAddAndEditImplementation.RUANG_CHAT, ruangChat)
                mBundle.putString(GroupAddAndEditImplementation.ADD_OR_EDIT, "EDIT")
                fragment.arguments = mBundle
                var transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.nav_host_fragment, fragment)
                transaction.commit()
            }

        })
    }

    //admin(sub/super admin) bisa menghapus,mengedit,dan menambah grup
    private fun getAllRoomsGroup() {
        arrayListRoom.clear()
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        val name = detailUser[SessionManager.KEY_NAMA].toString()

        var ruangChats: ArrayList<RuangChat>?

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.getRooms("Bearer $token")
            .enqueue(object : Callback<ResponseChatRoom> {
                override fun onResponse(
                    call: Call<ResponseChatRoom>,
                    response: Response<ResponseChatRoom>
                ) {

                    if (response.isSuccessful) {

                        ruangChats = response.body()?.chat_rooms
                        if (ruangChats != null) {
                            if (ruangChats!!.isEmpty()) {//anggapan setiap grup chat sudah ada satu member
                                //makeRoomChatPrivate()
                                Toast.makeText(
                                    requireActivity(),
                                    "belum ada group yang ditambahkan oleh admin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {

                                for (item in ruangChats!!) {
                                    if (item.type == "CHAT_ROOM:GROUP") {
                                        arrayListRoom.add(item)

                                    }
                                }
                                //ngambil semua grup chat dan semua admin bisa ngubah nama sama deskrispi aja
                                binding.progressbarGroupAdd.visibility=View.GONE
                                adapter.listRuangChat = arrayListRoom
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseChatRoom>, t: Throwable) {
                    binding.progressbarGroupAdd.visibility=View.GONE
                    Toast.makeText(requireActivity(), "${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGrupAddAndEditBinding.inflate(inflater)
        return binding.root
    }

}
