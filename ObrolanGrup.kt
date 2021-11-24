package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.RuangChatGroup
import id.ac.ipb.mobile.digitani.RuangChatPribadi
import id.ac.ipb.mobile.digitani.adapter.ObrolanGrupAdapter
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentObrolanGrupBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.GetChatMessages
import id.ac.ipb.mobile.digitani.model.MessageIsi
import id.ac.ipb.mobile.digitani.model.OneMessageForOneMember
import id.ac.ipb.mobile.digitani.model.RuangChat
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberData
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberResponse
import id.ac.ipb.mobile.digitani.response.ResponseChatRoom
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ObrolanGrup : Fragment() {

    private lateinit var rv_ruang_obrolan_grup: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ObrolanGrupAdapter
    private lateinit var listGrupChatPribadi: ArrayList<RuangChat>
    private lateinit var apiService: ApiInterface
    private lateinit var ruangChatTerakhir: ArrayList<RuangChat>
    private lateinit var mSocket: Socket
    private var initialization = false
    lateinit var isApproved: ArrayList<String>
    private lateinit var notifMuncul: MutableMap<String, Boolean>
    private lateinit var arrayListId: ArrayList<Int>
    private lateinit var arrayListOfSocketPribadi: ArrayList<Socket>
    private lateinit var onGetMessage: Emitter.Listener
    private lateinit var onConnect: Emitter.Listener
    private lateinit var onConnectUser: Emitter.Listener
    private lateinit var onJoiningRoom: Emitter.Listener
    private lateinit var onMessageSend: Emitter.Listener
    private lateinit var onJoiningRoomArrayList: ArrayList<Emitter.Listener>
    private lateinit var onGetMessageArrayList: ArrayList<Emitter.Listener>
    private lateinit var onMessageSendArrayList: ArrayList<Emitter.Listener>
    private lateinit var onConnectUserArrayList: ArrayList<Emitter.Listener>
    private lateinit var onConnectList: ArrayList<Emitter.Listener>
    private lateinit var userMemberStatus: MutableMap<String, ChatRoomMemberData>
    private var jumlahRuangChat: Int = 0
    private lateinit var arrayListRuang: ArrayList<Int>
    private lateinit var memberOfChatRoom: MutableMap<String, ArrayList<ChatRoomMemberData>>
    private lateinit var isAdminGrup: MutableMap<String, Int>
    private lateinit var grupYangPantas: ArrayList<RuangChat>
    private lateinit var userIsMemberOfGroup: ArrayList<Int>

    private lateinit var messageTerakhirInAllRoom: ArrayList<OneMessageForOneMember>
    private lateinit var messageTerakhirInMember: MutableMap<String, MessageIsi?>
    private var _binding: FragmentObrolanGrupBinding? = null
    private val binding get() = _binding!!
    private lateinit var messageAllInRoom: ArrayList<MessageIsi>
    private lateinit var job: Job
    private lateinit var userIsNotApprovedIn: ArrayList<ChatRoomMemberData>

    //menampilkan grup yang dia sudah masuk di dalamnya
    //menampilkan semua grup yang dia masuk di dalamnya baik sebagai member not approved or not
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(this.requireActivity())
        job = Job()
        grupYangPantas = arrayListOf()
        onConnectList = arrayListOf()
        onConnectUserArrayList = arrayListOf()
        onJoiningRoomArrayList = arrayListOf()
        onGetMessageArrayList = arrayListOf()
        onMessageSendArrayList = arrayListOf()
        arrayListOfSocketPribadi = arrayListOf()
        listGrupChatPribadi = arrayListOf()
        isAdminGrup = HashMap()
        ruangChatTerakhir = arrayListOf()
        memberOfChatRoom = HashMap()
        initialization = true
        userIsMemberOfGroup = arrayListOf()
        userMemberStatus = HashMap()
        isApproved = arrayListOf()
        arrayListRuang = arrayListOf()
        arrayListId = arrayListOf()
        notifMuncul = HashMap()
        messageTerakhirInMember = HashMap()
        messageTerakhirInAllRoom = arrayListOf()
        messageAllInRoom = arrayListOf()
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
        rv_ruang_obrolan_grup = binding.rvGroup
        binding.progressbarRiwayatGrup.visibility = View.VISIBLE
        userIsNotApprovedIn = arrayListOf()

        rv_ruang_obrolan_grup.layoutManager = LinearLayoutManager(this.context)
        adapter = ObrolanGrupAdapter(requireActivity())
        binding.rvGroup.adapter = adapter

        adapter.notifyDataSetChanged()
        adapter.setOnItemClickCallback(object : ObrolanGrupAdapter.OnItemClickCallback {
            override fun onItemClicked(ruangChat: RuangChat) {
                var member: ChatRoomMemberData = userMemberStatus[ruangChat.id.toString()]!!
                var isApproved: Boolean = member.is_approved
                var arrayListNotif: java.util.ArrayList<Boolean> = arrayListOf()
                notifMuncul[ruangChat.id.toString()] = false
                for (i in 0 until messageTerakhirInAllRoom.size) {
                    notifMuncul[messageTerakhirInAllRoom[i]!!.chat_room_id.toString()]?.let {
                        arrayListNotif.add(
                            it
                        )
                    }
                }
                adapter.listOfNotif = arrayListNotif
                Log.d("terakhir", "$arrayListNotif")
                adapter.notifyDataSetChanged()

                val intent = Intent(requireContext(), RuangChatGroup::class.java)
                intent.putExtra(RuangChatGroup.EXTRA_RUANG, ruangChat)
                intent.putExtra(RuangChatGroup.Is_Approved, isApproved)
                startActivity(intent)

            }

            override fun onLongItemClicked(ruangChat: RuangChat): Boolean {
                Toast.makeText(
                    requireActivity(),
                    "ruang chat ini ${ruangChat.name}",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }

        })
        binding.linearLayout.setOnClickListener {
            var f = GrupAllPeople()
            var transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_host_fragment, f)
            transaction.addToBackStack(null)
            transaction.commit()
        }
        if (arguments != null) {
            jumlahRuangChat = requireArguments().getInt(JUMLAH_GROUP_TERGABUNG)
            arrayListRuang =
                requireArguments().getIntegerArrayList(RUANG_CHAT_GROUP) as ArrayList<Int>
            isApproved = requireArguments().getStringArrayList(IS_APRROVED) as ArrayList<String>
            if (arrayListRuang.isNotEmpty()) {
                for (i in 0 until arrayListRuang.size) {

                    try {
                        //socket ditambahkan untuk setiap socket instance. Karena untuk listen masing2 harus ada 1 socket yang
                        //koneksi ke socket.io harus pake JWT
                        var opts: IO.Options = IO.Options()
                        opts.forceNew = true
                        opts.reconnection = true
                        opts.query = "token=$token"

                        mSocket = IO.socket(RuangChatPribadi.DIGITANI_SOCKET_URL, opts)
                        arrayListOfSocketPribadi.add(mSocket)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(RuangChatPribadi::class.java.simpleName, "Failed to connect")
                    }
                    //menyambungkan ke-n socket sesuai dengan jumlah ruangchatprivate dari activity, karena kalo activity nggak ngasih, maka akan tampil kosong

                    onConnect = Emitter.Listener {
                        Log.d("onConnect", "Terhubung$i")
                        var jsonObject = JSONObject()
                        try {
                            jsonObject.put("user_id", idUser)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(RuangChatPribadi::class.java.simpleName, "$e")
                        }
                        arrayListOfSocketPribadi[i].emit("user:connect", jsonObject)
                    }

                    onConnectList.add(onConnect)

                    onConnectUser = Emitter.Listener {
                        Log.d("onConnectUser", "menyambungkan $idUser ke ${arrayListRuang[i]}")
                        if (it[0] is Boolean && it[0] == false) {
                            requireActivity().runOnUiThread(Runnable {
                                var clientId = it as Array<*>
                                for (i in clientId) {
                                    Toast.makeText(
                                        requireActivity(),
                                        "user gagal terkoneksi dengan server.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                        } else {
                            var jsonObject = JSONObject()
                            try {
                                jsonObject.put("chat_room_id", arrayListRuang[i])
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e(RuangChatPribadi::class.java.simpleName, "$e")
                            }
                            arrayListOfSocketPribadi[i].emit("user:room:join", jsonObject)
                            //setelah user connect harus langsung join ke room correspond to ruangChat.id.toString()
                        }
                    }
                    onConnectUserArrayList.add(onConnectUser)

                    onJoiningRoom = Emitter.Listener {
                        var jsonObject = JSONObject()

                        try {
                            jsonObject.put("chat_room_id", arrayListRuang[i].toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(ObrolanPribadiFragment::class.java.simpleName, "$e")
                        }
                        arrayListOfSocketPribadi[i].emit("message:fetch", jsonObject)
                        Log.d("onJoiningRoom", arrayListRuang[i].toString())
                    }
                    onJoiningRoomArrayList.add(onJoiningRoom)

                    onGetMessage = Emitter.Listener { it ->
                        var sudahAdaYangNgirimChat = false
                     
                        var timeHasil: java.util.ArrayList<String>? = arrayListOf()
                        var messageSentCreated: String = ""
                        var date = Date()
                        //hanya id yang paling terkahir dikirimkan akan lebih besar dari pesan yang sudah dikirimkan
                        //diurutkan berdasarkan id
                        var messageTerakhirInARoomSimple: java.util.ArrayList<OneMessageForOneMember> =
                            arrayListOf()
                        //sudah coba diurutkan berdasarkan tanggal tapi tidak memenuhi hasilnya karena pada detik yang sama, lebih dari satu pesan dapat dikirimkan sehingga
                        //data yang masuk hanyalah sampai HH:mm:ss yang mana ss sama untuk sejumlah pesan dan bakalan nggak sesuai dengan data yang di server.
                        // memanfaatkan seluruh data yang sudah diurutkan di server yang hingga millisecond dapat diurutkan server. Maka data dari server sudah terurutkan
                        var arrayListTgl: java.util.ArrayList<Date> = arrayListOf()
                        var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        var messageInRoom: OneMessageForOneMember = OneMessageForOneMember(
                            0,
                            "",
                            "",
                            0,
                            "",
                            date,
                            "",
                            "",
                            0,
                            0,
                            false,
                            false
                        )
                        var detailUser = sessionManager?.userDetails()
                        var idUser = detailUser?.get(SessionManager.KEY_ID_USER) as Int
                        //menghandle message dari server socket.io yang masuk. Bukan sebagai pengirim
                        var messageTerhapusOlehThisUser: Boolean = false
                        var pesanSatuan = JSONObject()
                        var idPesanTerakhir = 0
                        var jsonArray: JSONArray = JSONArray()
                        if (it[0] is JSONArray) {
                            jsonArray = it[0] as JSONArray
                            if (jsonArray.length() != 0) {
                                sudahAdaYangNgirimChat = true
                            }
                            Log.d("isiPesan", "${it[0]}")
                            Log.d("jsonArray", "${jsonArray.length()}")
                        }

                        for (iterator in 0 until jsonArray.length()) {

                            //langsung dapat semua pesan dalam ruangan yang dimasukki
                            pesanSatuan = jsonArray.getJSONObject(iterator)
                            var chatMember = pesanSatuan.getJSONObject("chat_member")
                            var image = pesanSatuan.get("image")
                            var urlImage: String? = ""
                            var imageUrlFix: String = ""
                            var imageName: String? = ""

                            if (image is JSONObject && image != null) {
                                imageName = image.getString("original_name")
                                urlImage = image.getString("path")
                                if (urlImage is String && urlImage != null) {
                                    //olah gambar
                                }
                            }
                            var deletedByUser: String? = pesanSatuan.getString("deleted_by_str")
                            var createdAt: String = pesanSatuan.getString("created_at")

                            var content = pesanSatuan.getString("content")
                            var idMessage = pesanSatuan.getInt("id")

                            var userIdSender = chatMember.getInt("user_id")
                            var chatMemberId = pesanSatuan.getInt("chat_member_id")

                            var semuaKeteranganTerhapus: List<String>? = null
                            semuaKeteranganTerhapus = if (deletedByUser != null) {
                                deletedByUser?.split(",")
                            } else {
                                null
                            }
                            if (semuaKeteranganTerhapus != null) {
                                var isi: List<String> = arrayListOf()
                                for (thing in semuaKeteranganTerhapus) {
                                    isi = thing.split(":") as List<String>
                                    if (isi[0] == idUser.toString()) {
                                        messageTerhapusOlehThisUser = true
                                        break
                                    }
                                }
                            }

                            var stringDate = createdAt
                            timeHasil =
                                stringDate.split(".") as java.util.ArrayList<String>
                            messageSentCreated = timeHasil[0]
                            timeHasil =
                                messageSentCreated.split("T") as java.util.ArrayList<String>

                            messageSentCreated =
                                "${timeHasil[0]} ${timeHasil[1]}"
                            try {
                                date = formatter.parse(messageSentCreated)
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }

                            if (imageUrlFix != "") {
                                if (initialization) {
                                    messageInRoom = imageName?.let { it1 ->
                                        OneMessageForOneMember(
                                            idMessage,
                                            "CHAT_MESSAGE:WITH_IMAGE",
                                            content,
                                            chatMemberId,
                                            deletedByUser,
                                            date,
                                            it1,
                                            imageUrlFix,
                                            arrayListRuang[i],
                                            userIdSender,
                                            messageTerhapusOlehThisUser,
                                            false
                                        )
                                    }!!
                                }
                            } else {
                                if (initialization) {
                                    messageInRoom = OneMessageForOneMember(
                                        idMessage,
                                        "CHAT_MESSAGE:ONLY_TEXT",
                                        content,
                                        chatMemberId,
                                        deletedByUser,
                                        date,
                                        "",
                                        "",
                                        arrayListRuang[i],
                                        userIdSender,
                                        messageTerhapusOlehThisUser,
                                        false
                                    )
                                }
                            }
                            idPesanTerakhir = messageInRoom.id

                            if (!messageTerhapusOlehThisUser) {
                                messageTerakhirInARoomSimple.add(messageInRoom)
                            }
                            messageTerhapusOlehThisUser = false
                        }
                        var memberIsNotApproved = isApproved[i]
                        if (memberIsNotApproved == "isNotApproved") {
                            var mes = OneMessageForOneMember(
                                idPesanTerakhir,
                                "",
                                "isNotApproved",
                                0,
                                "",
                                date,
                                "",
                                "",
                                arrayListRuang[i],
                                0,
                                true,
                                false
                            )
                            messageTerakhirInAllRoom.add(mes)
                        } else if (sudahAdaYangNgirimChat) {
                            if (messageTerakhirInARoomSimple.isEmpty()) {

                                var mes =
                                    OneMessageForOneMember(
                                        idPesanTerakhir,
                                        "",
                                        "semuaSudahTerhapus",
                                        0,
                                        "",
                                        date,
                                        "",
                                        "",
                                        arrayListRuang[i],
                                        0,
                                        true,
                                        false
                                    )

                                messageTerakhirInAllRoom.add(mes)
                            } else {
                                messageTerakhirInAllRoom.add(messageTerakhirInARoomSimple[messageTerakhirInARoomSimple.size - 1])
                            }
                        } else {
                            var message =
                                OneMessageForOneMember(
                                    idPesanTerakhir,
                                    "",
                                    "nggakAdaYangNgirimChat",
                                    0,
                                    "",
                                    date,
                                    "",
                                    "",
                                    arrayListRuang[i],
                                    0,
                                    true,
                                    false
                                )

                            messageTerakhirInAllRoom.add(message)
                        }

                        Log.d(
                            "onGetMessage",
                            "$messageTerakhirInAllRoom"
                        )
                        if (messageTerakhirInAllRoom.size == arrayListRuang.size) {
                            arrayListId.clear()
                            if (initialization) {
                                getAllMembers()
                            }
                        }
                    }

                    onGetMessageArrayList.add(onGetMessage)

                    onMessageSend = Emitter.Listener { responseMessageUpdate ->
                        //mengambil semua pesan yang ada di ruangan ini yang terbaru dikirimkan oleh setiap user yang di ruangan
                        //terima semua message terupdate satu aja yang paling update. Tambahkan pada messagesInARoomSimple
                        var detailUser = sessionManager.userDetails()
                        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
                        var jsonObject = JSONObject()
                        var jsonObjectRuang = JSONObject()

                        if (responseMessageUpdate[0] is JSONObject) {
                            Log.d("hasil kiriman broadcas", "${responseMessageUpdate[0]}")
                            jsonObject = responseMessageUpdate[0] as JSONObject
                        } else {
                            Log.d("error", "${responseMessageUpdate[0]}")
                        }
                        var type = jsonObject.getString("type")
                        var idMember = jsonObject.getInt("chat_member_id")
                        var createdAt = jsonObject.getString("created_at")
                        var content = jsonObject.getString("content")
                        var idMessage = jsonObject.getInt("id")
                        var deletedByStr = null
                        var date = Date()
                        var chatRoomId: Int = 0
                        var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        var stringDate = createdAt
                        var timeHasil = stringDate.split(".") as java.util.ArrayList<String>
                        var messageSentCreated = timeHasil[0]
                        timeHasil =
                            messageSentCreated.split("T") as java.util.ArrayList<String>

                        messageSentCreated =
                            "${timeHasil[0]} ${timeHasil[1]}"
                        try {
                            date = formatter.parse(messageSentCreated)
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                        for (i1 in 0 until arrayListRuang.size) {
                            if (memberOfChatRoom.containsKey(arrayListRuang[i1].toString())) {
                                var isiRoom =
                                    memberOfChatRoom[arrayListRuang[i1].toString()] as java.util.ArrayList<ChatRoomMemberData>
                                if (isiRoom.firstOrNull { it.id == idMember } != null) {
                                    //ngambil id yang sesaui dengan id yang berubah dari server.
                                    var chatMember = isiRoom.firstOrNull { it.id == idMember }
                                    chatRoomId = arrayListRuang[i1]
                                }
                            }
                        }
                        var messageLastUpdate = OneMessageForOneMember(
                            idMessage,
                            type,
                            content,
                            idMember,
                            deletedByStr,
                            date,
                            "",
                            "",
                            chatRoomId,
                            idUser,
                            false,
                            false
                        )
                        for (i3 in 0 until messageTerakhirInAllRoom.size) {
                            if (messageTerakhirInAllRoom[i3]!!.chat_room_id == chatRoomId) {
                                var temp = messageTerakhirInAllRoom[i3]
                                messageTerakhirInAllRoom[i3] = messageLastUpdate

                                if (temp!!.id != messageTerakhirInAllRoom[i3]!!.chat_room_id) {
                                    //tampilkan hijau
                                    notifMuncul[chatRoomId.toString()] = true
                                    Log.d("notifMuncul", "$notifMuncul")
                                }
                            }
                        }
                        try {
                            if (initialization) {
                                jsonObjectRuang.put(
                                    "chat_room_id",
                                    arrayListRuang[i].toString()
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(requireActivity()::class.java.simpleName, "$e")
                        }
                        if (initialization) {
                            arrayListOfSocketPribadi[i].emit("message:fetch", jsonObjectRuang)
                        } else {
                            //saat terima pesan masuk, maka langsung update pesan yang terbaru saja diurutkan saja dari messageTerakhirInAllRoom
                            arrayListId.clear()

                            for (i0 in 0 until messageTerakhirInAllRoom.size) {
                                messageTerakhirInAllRoom[i0].let { it1 -> arrayListId.add(it1.id) }
                            }
                            //sudah paling akhir, baru urutkan chat dari yang paling terakhir dikirim
                            //paling terakhir ke paling atas supaya bisa nambah 5 terus.
                            arrayListId.sortDescending()
                            for (i1 in arrayListId.indices) {
                                var index =
                                    messageTerakhirInAllRoom.firstOrNull { it!!.id == arrayListId[i1] }
                                if (index != null) {

                                    var temp = index
                                    messageTerakhirInAllRoom[messageTerakhirInAllRoom.indexOf(
                                        index
                                    )] = messageTerakhirInAllRoom[i1]
                                    messageTerakhirInAllRoom[i1] = temp
                                }
                            }
                            var arrayListNotif: java.util.ArrayList<Boolean> = arrayListOf()

                            for (i3 in 0 until messageTerakhirInAllRoom.size) {
                                //semua message terakhir
                                notifMuncul[messageTerakhirInAllRoom[i3]!!.chat_room_id.toString()]?.let { it1 ->
                                    arrayListNotif.add(
                                        it1
                                    )
                                }

                                var temporary =
                                    ruangChatTerakhir.firstOrNull { it.id == messageTerakhirInAllRoom[i3].chat_room_id }
                                ruangChatTerakhir[ruangChatTerakhir.indexOf(temporary)] =
                                    ruangChatTerakhir[i3]
                                if (temporary != null) {
                                    ruangChatTerakhir[i3] = temporary
                                }

                            }
                            if (isVisible) {
                                requireActivity().runOnUiThread(Runnable {
                                    Log.d("onGetMessage1", "$messageTerakhirInAllRoom")
                                    adapter.listGroupChat = ruangChatTerakhir
                                    adapter.listMessageTerakhir = messageTerakhirInAllRoom
                                    adapter.listOfNotif = arrayListNotif
                                    adapter.notifyDataSetChanged()
                                })
                            }
                        }
                        //langsung saja pas yang ada di grup ini mendapatkan event message:send artinya setiap orang dalam grup ini mendapatkan refresh message.
                        //dibandingkan harus mengappend di messagesInARoomSimple better pada saat dapat refresh chat message, yaitu kalo ada yang ngirim pesan maka langsung
                    }
                    onMessageSendArrayList.add(onMessageSend)
                    arrayListOfSocketPribadi[i].connect()
                    arrayListOfSocketPribadi[i].on(Socket.EVENT_CONNECT, onConnectList[i])
                    arrayListOfSocketPribadi[i].on("user:connect", onConnectUserArrayList[i])
                    arrayListOfSocketPribadi[i].on("user:room:join", onJoiningRoomArrayList[i])
                    arrayListOfSocketPribadi[i].on("message:fetch", onGetMessageArrayList[i])
                    arrayListOfSocketPribadi[i].on("message:send", onMessageSendArrayList[i])

                }
            } else {
                binding.linearLayout.visibility = View.VISIBLE
                binding.progressbarRiwayatGrup.visibility = View.GONE
                var isiStatus =
                    requireActivity().resources.getString(R.string.group_null_adverb)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                    var status =
                        Html.fromHtml(isiStatus, Html.FROM_HTML_MODE_LEGACY)
                    binding.tvNullChat.text = status
                } else {
                    var statust =
                        requireActivity().resources.getString(R.string.group_n_chatting)
                    binding.tvNullChat.text = statust
                }
            }

        }
    }


    private fun getAllMembers() {
        val detailUser = sessionManager.userDetails()
        grupYangPantas.clear()
        listGrupChatPribadi.clear()
        isAdminGrup.clear()
        memberOfChatRoom.clear()
        userIsMemberOfGroup.clear()
        userMemberStatus.clear()
        initialization = false
        messageAllInRoom.clear()
        userIsNotApprovedIn.clear()

        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        val idUser = detailUser[SessionManager.KEY_ID_USER] as Int
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
                        //serta terima id chat group yang user yang sedang login adalah admin/member di dalamny
                        if (chatMembers != null) {
                            if (chatMembers!!.isEmpty()) {

                            } else {
                                for (chatRoomMember in chatMembers!!) {

                                    //identifikasi semua member dalam satu room, nggak tau ni room yang mana yang room group
                                    when {
                                        memberOfChatRoom.isEmpty() -> {
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember)
                                        }
                                        memberOfChatRoom.containsKey(chatRoomMember.chat_room_id.toString()) -> {
                                            var isiRuangChat =
                                                memberOfChatRoom[chatRoomMember.chat_room_id.toString()] as ArrayList<ChatRoomMemberData>
                                            isiRuangChat.add(chatRoomMember)
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                isiRuangChat
                                        }
                                        else -> {
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember)
                                        }
                                    }

                                    //mengambil user sekarang apakah dia merupakan member grup tersebut atau nggak
                                    if (userIsMemberOfGroup.isEmpty() && userMemberStatus.isEmpty()) {
                                        if (chatRoomMember.user_id == idUser) {
                                            userIsMemberOfGroup.add(chatRoomMember.chat_room_id)
                                            userMemberStatus[chatRoomMember.chat_room_id.toString()] =
                                                chatRoomMember
                                        }
                                    } else if (chatRoomMember.user_id == idUser) {
                                        userIsMemberOfGroup.add(chatRoomMember.chat_room_id)
                                        userMemberStatus[chatRoomMember.chat_room_id.toString()] =
                                            chatRoomMember
                                    }
                                    //mengambil seluruh data kememberan user di semua grup
                                    //ambil seluruh chatRoomMember yang dia not approved biar langsung pesan terakhirnya anda belom diizinkan masuk grup
                                    if (!chatRoomMember.is_approved && chatRoomMember.user_id == idUser) {
                                        userIsNotApprovedIn.add(chatRoomMember)
                                    }

                                }
                                //getAllMessagesInGroup()
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

    private fun getAllMessagesInGroup() {
   
        val detailUser = sessionManager!!.userDetails()
        var idUser = detailUser[SessionManager.KEY_ID_USER].toString()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var messageTerhapusOlehCurrentUser: Boolean = false
        apiService = ApiClientChatNew.chatRoomApiClient
        messageAllInRoom.clear()
        apiService.getAllMessage("Bearer $token")
            .enqueue(object : Callback<GetChatMessages> {
                override fun onResponse(
                    call: Call<GetChatMessages>,
                    response: Response<GetChatMessages>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            messageAllInRoom = response.body()?.chat_messages!!

                            for (message in messageAllInRoom) {
                        
                                var semuaKeteranganTerhapus: List<String>? = null
                                semuaKeteranganTerhapus = if (message.deleted_by_str != null) {
                                    message.deleted_by_str!!.split(",")
                                } else {
                                    null
                                }

                                //maksimal 499 jadi nggak O(n2) dan kalo sudah 500 hard delete yang pesan ini
                                if (semuaKeteranganTerhapus != null) {
                                    var isi: ArrayList<String> = arrayListOf()
                                    for (things in semuaKeteranganTerhapus) {
                                        isi = things.split(":") as ArrayList<String>
                                        if (isi[0] == idUser) {
                                            messageTerhapusOlehCurrentUser = true
                                            break
                                        }
                                    }
                                }
                                if (!messageTerakhirInMember.containsKey(message.chat_member_id.toString()) && !messageTerhapusOlehCurrentUser) {
                                    messageTerakhirInMember[message.chat_member_id.toString()] =
                                        message
                                } else if (!messageTerakhirInMember.containsKey(message.chat_member_id.toString()) && messageTerhapusOlehCurrentUser) {
                                    messageTerakhirInMember[message.chat_member_id.toString()] =
                                        null
                                } else if (messageTerakhirInMember.containsKey(message.chat_member_id.toString())) {
                                    if (!messageTerhapusOlehCurrentUser) {
                                        messageTerakhirInMember[message.chat_member_id.toString()] =
                                            message

                                    }
                                }
                                messageTerhapusOlehCurrentUser = false
                            }
                            //apakah dia tergabung dan approved? kalo approved tampilkan semua message dalam grup chat tersebut
                            //getAllRoomsGroup()
                        }

                    }
                }

                override fun onFailure(call: Call<GetChatMessages>, t: Throwable) {

                }
            })
    }

    //mengambil yang rooms group dahulu terus ambil semua message yang ada dalam grup tersebut
    private fun getAllRoomsGroup() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var ruangChats: ArrayList<RuangChat>? = null
        grupYangPantas.clear()
        apiService = ApiClientChatNew.chatRoomApiClient
    
        apiService.getRooms("Bearer $token")
            .enqueue(object : Callback<ResponseChatRoom> {
                override fun onResponse(
                    call: Call<ResponseChatRoom>,
                    response: Response<ResponseChatRoom>
                ) {

                    if (response.isSuccessful) {
                        ruangChats = response.body()?.chat_rooms
                        //cek terlebih dahulu apakah sudah tergabung ke dalam ruang chat pribadi, grup, dua duanya , atau nggak
                        //pisahkan dulu tergabung ke chat room nggak? kalo tidak tergabung ke ruang chat grup manapun, maka tampilkan textview

                        if (ruangChats != null) {
                            var isiRoom: ArrayList<Int> = arrayListOf()
                            if (ruangChats!!.isEmpty()) {

                                //tergantung juga apakah dia approved?
                                    
                                for (item in ruangChats!!) {
                                    if (item.type == "CHAT_ROOM:GROUP" && arrayListRuang.firstOrNull { it == item.id } != null) {
                                        //setiap ruangan itu harus mencari setiap pesan terakhirnya. Sekarang tersedia semua pesan terakhir semua grup member yang ada. Di Sort cari yang terakhir
                                        ruangChatTerakhir.add(item)
                                    }
                                }
                                arrayListId.clear()

                                for (i0 in 0 until messageTerakhirInAllRoom.size) {
                                    arrayListId.add(messageTerakhirInAllRoom[i0]!!.id)
                                }
                                arrayListId.sortDescending()
                                for (i1 in arrayListId.indices) {
                                    var index =
                                        messageTerakhirInAllRoom.firstOrNull { it!!.id == arrayListId[i1] }
                                    if (index != null) {

                                        var temp = index
                                        messageTerakhirInAllRoom[messageTerakhirInAllRoom.indexOf(
                                            index
                                        )] = messageTerakhirInAllRoom[i1]
                                        messageTerakhirInAllRoom[i1] = temp
                                    }

                                }
                                var arrayListNotif: ArrayList<Boolean> = arrayListOf()
                                Log.d("arrayListInit", "$arrayListId")
                                for (i in 0 until messageTerakhirInAllRoom.size) {
                                    notifMuncul[messageTerakhirInAllRoom[i]!!.chat_room_id.toString()] =
                                        false
                                    notifMuncul[messageTerakhirInAllRoom[i]!!.chat_room_id.toString()]?.let {
                                        arrayListNotif.add(
                                            it
                                        )
                                    }
                                    var temporary =
                                        ruangChatTerakhir.firstOrNull { it.id == messageTerakhirInAllRoom[i].chat_room_id }
                                    ruangChatTerakhir[ruangChatTerakhir.indexOf(temporary)] =
                                        ruangChatTerakhir[i]
                                    if (temporary != null) {
                                        ruangChatTerakhir[i] = temporary
                                    }

                                }

                                Log.d("onGetMessage", "$messageTerakhirInAllRoom")
                                Log.d("ruangChatTerakhir", "$ruangChatTerakhir")
                                adapter.listGroupChat = ruangChatTerakhir
                                adapter.listMessageTerakhir = messageTerakhirInAllRoom
                                if (adapter.listOfNotif.isEmpty()) {
                                    adapter.listOfNotif = arrayListNotif
                                }
                                adapter.notifyDataSetChanged()

                                binding.progressbarRiwayatGrup.visibility = View.GONE
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseChatRoom>, t: Throwable) {
                    binding.progressbarRiwayatGrup.visibility = View.GONE
                    Toast.makeText(requireActivity(), "${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentObrolanGrupBinding.inflate(inflater)
        val view = binding.root
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null

    }

    companion object {
        var RUANG_CHAT_GROUP = "ruang_chat_group"
        var IS_APRROVED = "is_aprroved"
        var JUMLAH_GROUP_TERGABUNG = "jumlah_group_tergabung"
    }
}
