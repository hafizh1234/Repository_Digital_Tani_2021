package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ipb.mobile.digitani.adapter.IzinMasukGrupAdapter
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentGrupAllPeopleBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.RuangChat
import id.ac.ipb.mobile.digitani.response.ChatMemberPostResponse
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberData
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberResponse
import id.ac.ipb.mobile.digitani.response.ResponseChatRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

class GrupAllPeople : Fragment(), CoroutineScope {
    private lateinit var adapter: IzinMasukGrupAdapter
    private lateinit var apiService: ApiInterface
    private lateinit var rvAllGroup: RecyclerView
    private var _binding: FragmentGrupAllPeopleBinding? = null
    private lateinit var job: Job
    private lateinit var isAdminGrup: MutableMap<String, Int>
    private lateinit var grupYangPantas: ArrayList<RuangChat>
    private lateinit var isMemberOfGroup: ArrayList<Int>
    private lateinit var memberOfChatRoom: MutableMap<String, ArrayList<Int>>
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    //tampilkan semua grup yang dia belom minta izin di dalamnya
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        grupYangPantas = arrayListOf()
        isAdminGrup = HashMap()
        memberOfChatRoom = HashMap()
        isMemberOfGroup = arrayListOf()

        rvAllGroup = binding.rvAllGroup
        job = Job()
        rvAllGroup.layoutManager = LinearLayoutManager(this.requireActivity())
        sessionManager = SessionManager(this.requireActivity())
        adapter = IzinMasukGrupAdapter(requireActivity())
        //adapter harus beda sama grup adapter karena mau bikin izin masuk
        rvAllGroup.adapter = adapter
        //loadAllGroup()

        getAllRoomsMember()
        binding.progressbarIzin.visibility=View.VISIBLE

        adapter.setOnItemClickCallback(object : IzinMasukGrupAdapter.OnItemClickCallback {
            override fun onItemClicked(ruangChat: RuangChat) {
                showDialog(ruangChat)
                 }
        })

    }

    private fun showDialog(ruangChat: RuangChat) {
        val dialogMessage: String =
            "Apakah anda ingin minta izin kepada admin untuk masuk ${ruangChat.name}?"
        val dialogTitle: String = "Izin Masuk Grup "

        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                postPermissionToAdmin(ruangChat)
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

    //hanya admin yang dapat melihat seluruh perizinan sehingga ini assign aja jadi group member tapi belum approved
    private fun postPermissionToAdmin(ruangChat: RuangChat) {
        var detailUser = sessionManager.userDetails()
        var memberTerbentuk: ChatRoomMemberData
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var id = detailUser[SessionManager.KEY_ID_USER] as Int
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.postMember(
            "Bearer $token",
            id.toString(),
            ruangChat.id.toString(),
            "CHAT_MEMBER:REGULAR"
        )
            .enqueue(object : Callback<ChatMemberPostResponse> {
                override fun onResponse(
                    call: Call<ChatMemberPostResponse>,
                    response: Response<ChatMemberPostResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireActivity(),
                            "Sukses meminta izin ke admin untuk masuk grup ${ruangChat.name}",
                            Toast.LENGTH_LONG
                        ).show()
                        memberTerbentuk = response.body()!!.chat_member
                        getAllRoomsMember()
                    }
                }
                override fun onFailure(call: Call<ChatMemberPostResponse>, t: Throwable) {

                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGrupAllPeopleBinding.inflate(inflater)
        return binding.root
    }

    private fun getAllRoomsMember() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        val apiInterface = ApiClientChatNew.chatRoomApiClient
        var chatMembers: ArrayList<ChatRoomMemberData>?

        apiInterface.getAllMembers("Bearer $token")
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        chatMembers = response.body()?.chat_members
                        if (chatMembers != null) {
                            if (chatMembers!!.isEmpty()) {

                            } else {
                                for (chatRoomMember in chatMembers!!) {
                                    //mengolah data chatRoomMembers untuk diambil data user sekarang yang sedang
                                    // aktif untuk diambil keanggotaannya (apakah seorang admin atau tidak, approved atau tidak)
                                }
                                getAllRoomsGroup()
                            }
                        } else {
                            //makeRoomChatPrivate()
                            Toast.makeText(
                                requireActivity(),
                                "tidak ada member grup sama sekali",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }

                override fun onFailure(call: Call<ChatRoomMemberResponse>, t: Throwable) {
                    Toast.makeText(
                        requireActivity(),
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun getAllRoomsGroup() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        grupYangPantas.clear()
        var ruangChats: ArrayList<RuangChat>?
        var ruangChatPrivates: ArrayList<RuangChat>? = arrayListOf()

        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getRooms("Bearer $token")
            .enqueue(object : Callback<ResponseChatRoom> {
                override fun onResponse(
                    call: Call<ResponseChatRoom>,
                    response: Response<ResponseChatRoom>
                ) {

                    if (response.isSuccessful) {
                        ruangChats = response.body()?.chat_rooms
                        binding.progressbarIzin.visibility=View.GONE
                        if (ruangChats != null) {
                            var isiRoom: ArrayList<Int> = arrayListOf()
                            if (ruangChats!!.isEmpty()) {
                                Toast.makeText(
                                    requireActivity(),
                                    "belum ada group yang ditambahkan oleh admin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {

                                for (item in ruangChats!!) {
                                    if (item.type == "CHAT_ROOM:GROUP" && isMemberOfGroup.firstOrNull { it == item.id } == null) {
                                        grupYangPantas.add(item)
                                    }
                                }
                                adapter.listGroupAll = grupYangPantas
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseChatRoom>, t: Throwable) {
                    binding.progressbarIzin.visibility=View.GONE
                    Toast.makeText(requireActivity(), "${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job
}