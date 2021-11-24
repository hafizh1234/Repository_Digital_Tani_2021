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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.RuangChatPribadi
import id.ac.ipb.mobile.digitani.adapter.ObrolanPribadiAdapter
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentObrolanPribadiBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.*
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
import kotlin.coroutines.CoroutineContext

class ObrolanPribadiFragment : Fragment() {
    private lateinit var rv_ruang_obrolan_grup: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ObrolanPribadiAdapter
    private lateinit var apiService: ApiInterface
    private var _binding: FragmentObrolanPribadiBinding? = null
    private lateinit var isiPesanDiruangan: MutableMap<String, ArrayList<OneMessageForOneMember>>
    private val binding get() = _binding!!
    private var initialization = true
    private lateinit var arrayListOfSocketPribadi: ArrayList<Socket>
    private lateinit var ruangChatPrivate: ArrayList<Int>
    private lateinit var mSocket: Socket
    private lateinit var notifMuncul: MutableMap<String, Boolean>
    private lateinit var onMessageSend: Emitter.Listener
    private lateinit var onJoiningRoom: Emitter.Listener
    private lateinit var onConnectUser: Emitter.Listener
    private lateinit var jumlahOrangPerGrup: MutableMap<String, Int>
    private var jumlahRuangChatPrivate: Int = 0
    private lateinit var arrayListId: ArrayList<Int>
    private lateinit var onConnectList: ArrayList<Emitter.Listener>
    private lateinit var isAdminGrupPrivate: MutableMap<String, Any>
    private lateinit var onConnectUserArrayList: ArrayList<Emitter.Listener>
    private lateinit var onConnect: Emitter.Listener
    private lateinit var onGetMessage: Emitter.Listener
    private lateinit var onMessageSendArrayList: ArrayList<Emitter.Listener>
    private lateinit var onGetMessageArrayList: ArrayList<Emitter.Listener>
    private lateinit var onJoiningRoomArrayList: ArrayList<Emitter.Listener>
    private lateinit var usersAllEvery: ArrayList<User>
    private lateinit var messageAllInRoom: ArrayList<MessageIsi>

    private lateinit var isMemberOfGroup: ArrayList<Int>
    private lateinit var usersAll: MutableMap<String, ArrayList<String>>
    private lateinit var memberOfPrivateRoomChat: MutableMap<String, ArrayList<ChatRoomMemberData>>
    private lateinit var messageTerakhirInMember: MutableMap<String, MessageIsi?>
    private lateinit var messageTerakhirInAllRoom: ArrayList<OneMessageForOneMember>
    //socket.io harus diinisialisasikan berapa banyak ruangan yang dia sudah tergabung terlebih dahulu.
    //socket.io nggak bisa dari sini untuk menentukan berapa banyak ruangan serta instance dari socket.io sendiri untuk menentukan berapa banyak arrayList yang harus dibikin.

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(this.requireActivity())
        isAdminGrupPrivate = HashMap()
        jumlahOrangPerGrup = HashMap()
        messageAllInRoom = arrayListOf()
        usersAllEvery = arrayListOf()
        arrayListOfSocketPribadi = arrayListOf()
        ruangChatPrivate = arrayListOf()
        notifMuncul = HashMap()
        arrayListId = arrayListOf()
        jumlahRuangChatPrivate = 0

        messageTerakhirInAllRoom = arrayListOf()
        onConnectList = arrayListOf()
        onConnectUserArrayList = arrayListOf()
        onMessageSendArrayList = arrayListOf()
        onGetMessageArrayList = arrayListOf()
        onJoiningRoomArrayList = arrayListOf()
        messageTerakhirInMember = HashMap()
        isMemberOfGroup = arrayListOf()
        usersAll = HashMap()
        memberOfPrivateRoomChat = HashMap()
        isiPesanDiruangan = HashMap()

        rv_ruang_obrolan_grup = binding.rvPrivateChat
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
        rv_ruang_obrolan_grup.layoutManager = LinearLayoutManager(this.context)
        adapter = ObrolanPribadiAdapter(this.requireActivity())
        binding.rvPrivateChat.adapter = adapter
        binding.progressbarRiwayatPribadi.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
        adapter.setOnItemClickCallbackPrivate(object :
            ObrolanPribadiAdapter.OnItemClickCallbackPrivate {
            override fun onItemClicked(ruangChat: RuangChat) {
                var arrayListNotif: ArrayList<Boolean> = arrayListOf()
                notifMuncul[ruangChat.id.toString()] = false
                for (i in 0 until messageTerakhirInAllRoom.size) {
                    notifMuncul[messageTerakhirInAllRoom[i].chat_room_id.toString()]?.let {
                        arrayListNotif.add(
                            it
                        )
                    }
                }
                adapter.listOfNotif = arrayListNotif
                adapter.notifyDataSetChanged()
                val intent = Intent(requireContext(), RuangChatPribadi::class.java)
                intent.putExtra(RuangChatPribadi.EXTRA_RUANG, ruangChat)
                startActivity(intent)

            }

            override fun onLongItemClicked(ruangChat: RuangChat): Boolean {
                return true
            }

        })
        //digunakan untuk menghapus chat pribadi apakah dia adalah admin dari member sama group untuk menghapus chat liat apakah tidak ada lagi isi groupnya?
        //jika di dalam group ada orang maka catat aja tanggal di delete pada saat user ingin mendelete log chat pribadinya karena
        //user yang lain harus ada log chatnya
        binding.ll.setOnClickListener {
            var fragment = PrivateChatAll()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_host_fragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        if (arguments != null) {
            ruangChatPrivate =
                requireArguments().getIntegerArrayList(GRUP_CHAT_PRIVATE) as ArrayList<Int>
            jumlahRuangChatPrivate = requireArguments().getInt(JUMLAH_PRIVATE_CHAT)
            //Toast.makeText(requireContext(),"$ruangChatPrivate,$jumlahRuangChatPrivate",Toast.LENGTH_LONG).show()
            if (ruangChatPrivate.isNotEmpty()) {
                for (i in 0 until ruangChatPrivate.size) {

                    //var mSocket:Socket=IO.socket("")
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
                    //menyambungkan ke-n socket sesuai dengan jumlah ruangchatprivate dari activity, karena kalo activity tidak memberi, maka akan tampil kosong

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
                        Log.d("onConnectUser", "menyambungkan $idUser ke ${ruangChatPrivate[i]}")
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
                                jsonObject.put("chat_room_id", ruangChatPrivate[i])
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
                            jsonObject.put("chat_room_id", ruangChatPrivate[i].toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(ObrolanPribadiFragment::class.java.simpleName, "$e")
                        }
                        arrayListOfSocketPribadi[i].emit("message:fetch", jsonObject)
                        Log.d("onJoiningRoom", ruangChatPrivate[i].toString())
                    }
                    onJoiningRoomArrayList.add(onJoiningRoom)

                    onGetMessage = Emitter.Listener { it ->
                        var sudahAdaYangNgirimChat = false
                        //messagesInARoomSimple.clear()

                        var timeHasil: ArrayList<String>? = arrayListOf()
                        var messageSentCreated: String = ""
                        var date = Date()
                        //hanya id yang paling terkahir dikirimkan akan lebih besar dari pesan yang sudah dikirimkan
                        //diurutkan berdasarkan id
                        var messageTerakhirInARoomSimple: ArrayList<OneMessageForOneMember> =
                            arrayListOf()
                        //var arrayListDate=Date()
                        //data yang masuk hanyalah sampai HH:mm:ss yang mana ss sama untuk sejumlah pesan dan bakalan nggak sesuai dengan data yang di server.
                        // memanfaatkan seluruh data yang sudah diurutkan di server yang hingga millisecond dapat diurutkan server. Maka data dari server sudah terurutkan
                        var arrayListTgl: ArrayList<Date> = arrayListOf()
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
                                   //pengoilahan image
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
                                stringDate.split(".") as ArrayList<String>
                            messageSentCreated = timeHasil[0]
                            timeHasil =
                                messageSentCreated.split("T") as ArrayList<String>

                            messageSentCreated =
                                "${timeHasil[0]} ${timeHasil[1]}"
                            try {
                                date = formatter.parse(messageSentCreated)
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }

                            if (imageUrlFix != "") {
                                //type="CHAT_MESSAGE:WITH_IMAGE"
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
                                            ruangChatPrivate[i],
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
                                        ruangChatPrivate[i],
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
                        if (sudahAdaYangNgirimChat) {
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
                                        ruangChatPrivate[i],
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
                                    ruangChatPrivate[i],
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
                        if (messageTerakhirInAllRoom.size == ruangChatPrivate.size) {
                            arrayListId.clear()
                            if (initialization) {
                                getAllRoomsMember()
                            }
                        }
                    }

                    onGetMessageArrayList.add(onGetMessage)

                    onMessageSend = Emitter.Listener {
                        //ngambil semua pesan yang ada di ruangan ini yang terbaru dikirimkan oleh setiap user yang di ruangan
                        //terima semua message terupdate satu aja yang paling update. Tambahkan pada messagesInARoomSimple
                        var detailUser = sessionManager.userDetails()
                        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
                        var jsonObject = JSONObject()
                        var jsonObjectRuang = JSONObject()

                        if (it[0] is JSONObject) {
                            Log.d("hasil kiriman broadcas", "${it[0]}")
                            jsonObject = it[0] as JSONObject
                        } else {
                            Log.d("error", "${it[0]}")
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
                        var timeHasil = stringDate.split(".") as ArrayList<String>
                        var messageSentCreated = timeHasil[0]
                        timeHasil =
                            messageSentCreated.split("T") as ArrayList<String>

                        messageSentCreated =
                            "${timeHasil[0]} ${timeHasil[1]}"
                        try {
                            date = formatter.parse(messageSentCreated)
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                        for (i1 in 0 until ruangChatPrivate.size) {
                            if (memberOfPrivateRoomChat.containsKey(ruangChatPrivate[i1].toString())) {
                                var isiRoom =
                                    memberOfPrivateRoomChat[ruangChatPrivate[i1].toString()] as ArrayList<ChatRoomMemberData>
                                if (isiRoom.firstOrNull { it.id == idMember } != null) {
                                    //ngambil id yang sesaui dengan id yang berubah dari server.
                                    var chatMember = isiRoom.firstOrNull { it.id == idMember }
                                    chatRoomId = ruangChatPrivate[i1]
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
                            if (messageTerakhirInAllRoom[i3].chat_room_id == chatRoomId) {
                                var temp = messageTerakhirInAllRoom[i3]
                                messageTerakhirInAllRoom[i3] = messageLastUpdate

                                if (temp.id != messageTerakhirInAllRoom[i3].chat_room_id) {
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
                                    ruangChatPrivate[i].toString()
                                )
                            } else {

                                //terimaPesan = true
                                //jsonObjectRuang.put("chat_room_id", ruangChatPrivate[i9])
                                //Log.d("room", "${ruangChatPrivate[i9]}")
                                //indexRuangGlobal = i9
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(requireActivity()::class.java.simpleName, "$e")
                        }
                        if (initialization) {
                            arrayListOfSocketPribadi[i].emit("message:fetch", jsonObjectRuang)
                        } else {
                            //saat terima pesan masuk, maka langsung update pesan yang terbaru aja diurutin aja dari messageTerakhirInAllRoom
                            arrayListId.clear()
                            var ruangChatTerakhir: ArrayList<RuangChat> = arrayListOf()

                            for (i0 in 0 until messageTerakhirInAllRoom.size) {
                                arrayListId.add(messageTerakhirInAllRoom[i0].id)
                            }
                            //sudah paling akhir, baru urutkan chat dari yang paling terakhir dikirim
                            //paling terakhir ke paling atas supaya bisa nambah 5 terus.
                            arrayListId.sortDescending()
                            for (i1 in arrayListId.indices) {
                                var index =
                                    messageTerakhirInAllRoom.firstOrNull { it.id == arrayListId[i1] }
                                if (index != null) {

                                    var temp = index
                                    messageTerakhirInAllRoom[messageTerakhirInAllRoom.indexOf(
                                        index
                                    )] = messageTerakhirInAllRoom[i1]
                                    messageTerakhirInAllRoom[i1] = temp
                                }
                            }
                            var arrayListNotif: ArrayList<Boolean> = arrayListOf()

                            for (i3 in 0 until messageTerakhirInAllRoom.size) {
                                //semua message terakhir
                                notifMuncul[messageTerakhirInAllRoom[i3].chat_room_id.toString()]?.let { it1 ->
                                    arrayListNotif.add(
                                        it1
                                    )
                                }

                                var hasilRuangChat = RuangChat(0, "", "", "", 0)
                                if (memberOfPrivateRoomChat[messageTerakhirInAllRoom[i3].chat_room_id.toString()] != null) {
                                    var memberdiRuang =
                                        memberOfPrivateRoomChat[messageTerakhirInAllRoom[i3].chat_room_id.toString()] as ArrayList<ChatRoomMemberData>
                                    Log.d("messageTerakhir!init", "${messageTerakhirInAllRoom[i3]}")
                                    Log.d("memberRuang!init", "$memberdiRuang")
                                    var temanNgobrol =
                                        memberdiRuang.firstOrNull { it.user_id != idUser }
                                    var userTeman =
                                        usersAllEvery.firstOrNull { it.id == temanNgobrol?.user_id ?: 0 }
                                    if (userTeman != null) {
                                        hasilRuangChat = RuangChat(
                                            messageTerakhirInAllRoom[i3].chat_room_id,
                                            "${userTeman.username} (${userTeman.name})",
                                            "CHAT_ROOM:PRIVATE",
                                            "${userTeman.kategoriUser}",
                                            0
                                        )
                                    }
                                    //Toast.makeText(requireActivity(),"${user.name}",Toast.LENGTH_LONG).show()
                                    ruangChatTerakhir.add(hasilRuangChat)
                                }
                            }
                            if (isVisible) {
                                requireActivity().runOnUiThread(Runnable {
                                    Log.d("onGetMessage1", "$messageTerakhirInAllRoom")
                                    adapter.listPrivateChat = ruangChatTerakhir
                                    adapter.listMessage = messageTerakhirInAllRoom
                                    adapter.listOfNotif = arrayListNotif
                                    adapter.notifyDataSetChanged()
                                })

                            }
                        }
                        //langsung aja pas yang ada di grup ini mendapatkan event message:send artinya setiap orang dalam grup ini mendapatkan refresh message.
                        //dibandingkan harus mengappend di messagesInARoomSimple mendingan pada saat dapat refresh chat message, yaitu kalo ada yang ngirim pesan maka langsung
                    }
                    onMessageSendArrayList.add(onMessageSend)
                    //arrayListOfSocketPribadi.add(mSocket)
                    arrayListOfSocketPribadi[i].connect()
                    arrayListOfSocketPribadi[i].on(Socket.EVENT_CONNECT, onConnectList[i])
                    arrayListOfSocketPribadi[i].on("user:connect", onConnectUserArrayList[i])
                    arrayListOfSocketPribadi[i].on("user:room:join", onJoiningRoomArrayList[i])
                    arrayListOfSocketPribadi[i].on("message:fetch", onGetMessageArrayList[i])
                    arrayListOfSocketPribadi[i].on("message:send", onMessageSendArrayList[i])


                }
            } else {
                binding.ll.visibility = View.VISIBLE
                var isiText =
                    requireActivity().resources.getString(R.string.null_chatting)

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                    var status =
                        Html.fromHtml(isiText, Html.FROM_HTML_MODE_LEGACY)
                    binding.tvIconWhenRvIsNull.text = status
                } else {
                    var text =
                        requireActivity().resources.getString(R.string.null_chatting_n)
                    binding.tvIconWhenRvIsNull.text = text
                }
                binding.progressbarRiwayatPribadi.visibility = View.GONE
            }
        }
    }


    private fun loadPrivateChatRoom() {
        val detailUser = sessionManager.userDetails()
        var semuaPesanDihapusDiRuangan = true
        var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var messageSentCreated: String
        var timeHasil: ArrayList<String> = arrayListOf()
        var date: Date = Date()
        var messageTerakhirInThisGroup: OneMessageForOneMember = OneMessageForOneMember(
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
        val userId = detailUser[SessionManager.KEY_ID_USER] as Int
        var satuOrangPunNggakAdaYangNgirimChat = true
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        //Log.d(ObrolanPribadiFragment::class.java.simpleName, token)
        var ruangChats: ArrayList<RuangChat>?
        val ruangChatGroup: ArrayList<RuangChat>? = arrayListOf<RuangChat>()
        var hasilRuangChat: RuangChat
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
                            if (ruangChats!!.isEmpty()) {
                                Toast.makeText(
                                    requireActivity(),
                                    "tidak ada ruang chat sama sekali",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                for (item in ruangChats!!) {//mengambil room yang private dan dia tergabung di dalamnya
                                    if (item.type == "CHAT_ROOM:PRIVATE" && isMemberOfGroup.firstOrNull { it == item.id } != null && jumlahOrangPerGrup[item.id.toString()] == 2) {
                                        //ini isinya data dari chat terakhir desc
                                        val isiRoom =
                                            memberOfPrivateRoomChat[item.id.toString()] as ArrayList<ChatRoomMemberData>

                                        var userIdTemanNgobrol =
                                            isiRoom.filterNot { it.user_id == userId } as ArrayList<ChatRoomMemberData>
                                        //var nameUserAndRole: ArrayList<String> = usersAll[userIdTemanNgobrol[0].toString()] as ArrayList<String>
                                        var user =
                                            usersAllEvery.firstOrNull { it.id == userIdTemanNgobrol[0].user_id }
                                        for (roomChatMember in isiRoom) {
                                            var messageMember =
                                                messageTerakhirInMember.containsKey(roomChatMember.id.toString())
                                            //1. nggak ada yang ngirim chat
                                            //2. sudah dihapus semua oleh user ini semua chat di ruangan.
                                            //3. user sudah mengirim chat dan masih ada chatnya
                                            //
                                            //dia melihat semua member dalam room ini siapa yang ngechat paling terakhir ambil message terakhir itu.
                                            //nyari di dalam member in group apakah dia sudah pernah kirim message belom?kalo sudah baru consider di dalamnya null(pernah dihapus sama user ini atau nggak(belom dihapus sama user ini)
                                            //kalo dia belom pernah ngirim pesan di grup ini, maka messageTerakhirInMember itu bakalan nggak ada yang ngandung keynya id member dia
                                            if (messageMember) {
                                                satuOrangPunNggakAdaYangNgirimChat = false
                                                //pesan ada dan salah seorang member sudah mengirimkan pesan tapi pesan itu sudah terhapus sama member ini this.member
                                                if (messageTerakhirInMember[roomChatMember.id.toString()] == null) {
                                                    if (semuaPesanDihapusDiRuangan == true) {
                                                        semuaPesanDihapusDiRuangan = true
                                                        if (roomChatMember == isiRoom[isiRoom.size - 1]) {
                                                            //semua member pesan terakhirnya sudah dihapus sama user Cureent
                                                            var date = Date()
                                                            messageTerakhirInThisGroup =
                                                                OneMessageForOneMember(
                                                                    0,
                                                                    "",
                                                                    "semuaSudahTerhapus",
                                                                    0,
                                                                    "",
                                                                    date,
                                                                    "",
                                                                    "",
                                                                    0,
                                                                    0,
                                                                    true,
                                                                    false
                                                                )
                                                        }
                                                    }
                                                } else {
                                                    if (messageTerakhirInMember[roomChatMember.id.toString()] != null) {
                                                        var messageTerakhir =
                                                            messageTerakhirInMember[roomChatMember.id.toString()]
                                                        semuaPesanDihapusDiRuangan = false
                                                        //setelah dapat semua messagesInThisRoom maka cari
                                                        //update pesan latest dari setiap member sehingga setiap pesan paling terakhir dari setiap member keliatan yang paling terakhir
                                                        //jadi pesan terakhir di group
                                                        var stringDate =
                                                            messageTerakhir?.created_at
                                                        timeHasil =
                                                            stringDate?.split(".") as ArrayList<String>
                                                        messageSentCreated = timeHasil[0]
                                                        timeHasil =
                                                            messageSentCreated.split("T") as ArrayList<String>
                                                        //waktu pesan member terbaru terkirim oleh member, pesan terakhir ngambilnya
                                                        messageSentCreated =
                                                            "${timeHasil[0]} ${timeHasil[1]}"
                                                        //current date message in member terakhir yang adalah grup member tersebut
                                                        try {
                                                            date =
                                                                formatter.parse(
                                                                    messageSentCreated
                                                                )
                                                        } catch (e: ParseException) {
                                                            e.printStackTrace()
                                                        }
                                                        if (messageTerakhirInThisGroup == null) {
                                                            if (messageTerakhir != null) {
                                                                messageTerakhirInThisGroup =
                                                                    date?.let {
                                                                        OneMessageForOneMember(
                                                                            messageTerakhir.id,
                                                                            messageTerakhir.type,
                                                                            messageTerakhir.content,
                                                                            messageTerakhir.chat_member_id,
                                                                            messageTerakhir.deleted_by_str,
                                                                            it,
                                                                            "",
                                                                            "",
                                                                            roomChatMember.chat_room_id,
                                                                            roomChatMember.user_id,
                                                                            false,
                                                                            false
                                                                        )

                                                                    }
                                                            }
                                                        } else {
                                                            var stringDate1 =
                                                                messageTerakhirInThisGroup?.created_at
                                                            //adalah stringDate yang sudah disimpan yang merupakan messageTerakhir
                                                            if (date!! > stringDate1) {
                                                                //jika date dari messageTerakhirInGroup itu adalah latest maka update messageTerakhir In Thjs Group
                                                                if (messageTerakhir != null) {
                                                                    messageTerakhirInThisGroup =
                                                                        date?.let {
                                                                            OneMessageForOneMember(
                                                                                messageTerakhir.id,
                                                                                messageTerakhir.type,
                                                                                messageTerakhir.content,
                                                                                messageTerakhir.chat_member_id,
                                                                                messageTerakhir.deleted_by_str,
                                                                                it,
                                                                                "",
                                                                                "",
                                                                                roomChatMember.chat_room_id,
                                                                                roomChatMember.user_id,
                                                                                false,
                                                                                false
                                                                            )

                                                                        }
                                                                }

                                                            }
                                                        }

                                                    }
                                                }
                                            } else if (satuOrangPunNggakAdaYangNgirimChat && roomChatMember == isiRoom[isiRoom.size - 1]) {
                                                var date = Date()
                                                messageTerakhirInThisGroup =
                                                    OneMessageForOneMember(
                                                        0,
                                                        "",
                                                        "nggakAdaYangNgirimChat",
                                                        0,
                                                        "",
                                                        date,
                                                        "",
                                                        "",
                                                        0,
                                                        0,
                                                        true,
                                                        false
                                                    )

                                            }


                                        }
                                        //message terakhir dari semua member itu messagesInROom
                                        //cari yang terakhir
                                        messageTerakhirInAllRoom.add(messageTerakhirInThisGroup)

                                        satuOrangPunNggakAdaYangNgirimChat = true
                                        //var nameOriginal = nameUserAndRole[0]
                                        //var kategoriUserTeman = nameUserAndRole[1]
                                        //Toast.makeText(requireActivity(),"nama temen ${nameOriginal.toString()}",Toast.LENGTH_SHORT).show()
                                        //description seharusnya pesan terakhir
                                        //Toast.makeText(requireActivity(),"user id teman $userIdTemanNgobrol ngobrol",Toast.LENGTH_SHORT).show()
                                        if (user != null) {
                                            hasilRuangChat = RuangChat(
                                                item.id,
                                                "${user.username} (${user.name})",
                                                item.type,
                                                "${user.kategoriUser}",
                                                item.updated_by_id
                                            )
                                            //Toast.makeText(requireActivity(),"${user.name}",Toast.LENGTH_LONG).show()
                                            ruangChatGroup?.add(hasilRuangChat)
                                        }
                                    }
                                    //untuk yang merupakan room admin, cari jumlah anggota kalo 1 berarti yang satunya lagi udah ngehapus pesan
                                }
                                adapter.listMessage = messageTerakhirInAllRoom
                                adapter.listPrivateChat = ruangChatGroup as ArrayList<RuangChat>
                                adapter.notifyDataSetChanged()
                                if (ruangChatGroup.isEmpty()) {
                                    binding.ll.visibility = View.VISIBLE
                                    var isiText =
                                        requireActivity().resources.getString(R.string.null_chatting)

                                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                                        var status =
                                            Html.fromHtml(isiText, Html.FROM_HTML_MODE_LEGACY)
                                        binding.tvIconWhenRvIsNull.text = status
                                    } else {
                                        var text =
                                            requireActivity().resources.getString(R.string.null_chatting_n)
                                        binding.tvIconWhenRvIsNull.text = text
                                    }
                                } else {
                                    binding.ll.visibility = View.GONE

                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@ObrolanPribadiFragment.requireContext(),
                            "ada kesalahan, yaitu ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseChatRoom>, t: Throwable) {
                    Toast.makeText(context, "${t.message}", Toast.LENGTH_SHORT).show()

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
        //ambil semua message
        //filter room yang sesuai
        apiService.getAllMessage("Bearer $token")
            .enqueue(object : Callback<GetChatMessages> {
                override fun onResponse(
                    call: Call<GetChatMessages>,
                    response: Response<GetChatMessages>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            messageAllInRoom = response.body()?.chat_messages!!

                            //ambil semua pesan sisihkan semua pesan dalam satu ruangan yang sama
                            //untuk semua message akan diberlakukan
                            //semua pesan terakhir dari setiap member diambil kecuali pesan itu sudah di delete sama user yang sekarang
                            for (message in messageAllInRoom) {
                                //isinya member apapun yang terakhir message dari dia langsung timpa aja.
                                //dicek dulu current messagenya ini sudah dihapus belom sama user yang sekarang, kalo udah berarti pertahankan message yang lama

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
                                    //bakalan null kalo dia belom punya message yaitu kalo messagenya semua dihapus
                                } else if (messageTerakhirInMember.containsKey(message.chat_member_id.toString())) {
                                    if (!messageTerhapusOlehCurrentUser) {
                                        messageTerakhirInMember[message.chat_member_id.toString()] =
                                            message
                                        //kalo dia ada maka ada dua kemungkinan isinya null, yaitu dia baru ketemu pesan yang dia hapus sendiri sebelumnya dan pastinya isinya null
                                        //atau dia pernah ketemu pesan yang dia belom hapus isinya,yaitu adalah message terakhir yang belom kehapus
                                    }//else{
                                    //messageTerakhirInMember menyimpan message yang paling akhir yang pernah dikirimkan oleh member ini,
                                    //tapi conditional ini adalah kondisi yang ketemu pesan lagi tapi pesan itu sudah dihapus oleh member sekarang
                                    //sehingga pesan terakhir tetep aja yang terakhir dihapus. Ini adalah mengconsider semua pesan yang sudah dikirim bagi member yang belom mengirim pesan tidak akan memengaruhi apapun
                                    //}
                                }
                                messageTerhapusOlehCurrentUser = false
                                //selesai semua maka messageTerhapusnya kembalikan ke false untuk ngecek message selanjutnya pada AllMesage
                            }

                            //sudah dapat semua pesan yang dikirimkan semua orang dalam grup, yaitu member maka harus dilihat lagi semua grup chat yang dia tergabung di dalamnya
                            //apakah dia tergabung dan approved? kalo approved tampilkan semua message dalam grup chat tersebut

                        }
                        //loadAllUser()

                    }
                }

                override fun onFailure(call: Call<GetChatMessages>, t: Throwable) {

                }
            })
    }

    private fun showDialog(keterangan: String, pesan: OneMessageForOneMember?) {
        var dialogMessage: String = ""
        var dialogTitle: String = ""
        if (keterangan == "tolakPertemanan") {
            dialogMessage =
                "Apakah anda ingin menolak permintaan pertemanan dari user ini? Jika iya, maka seluruh pesan anda dengan user ini akan terhapus."
            dialogTitle = "Penolakan Permintaan Pertemanan"
        } else if (keterangan == "hapusPesan") {
            dialogMessage = "Apakah anda ingin menghapus pesan ini?"
            dialogTitle = "Hapus Pesan"
        }
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->

                if (keterangan == "hapusSemuaChat") {

                }

            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    companion object {
        var GRUP_CHAT_PRIVATE = "grup_chat_private"
        var JUMLAH_PRIVATE_CHAT = "jumlah_private_chat"
    }

    private fun loadAllUser() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
        var users: ArrayList<UserLoginData>
        var kategori: UserCategory? = null
        var peran: String = ""
        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        users = response.body()?.users!!

                        if (users.isEmpty()) {
                            Toast.makeText(requireActivity(), "Tidak ada user", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            for (item in users) {
                                if (!item.roles?.isEmpty()!!) {
                                    kategori = item.roles!![0]
                                    peran = kategori!!.name.toString()
                                } else {
                                    peran = "Peran belum diatur"
                                }
                                var kategoriUser = ""
                                val stringRole = resources.getStringArray(R.array.RoleAll)
                                kategoriUser =
                                    when (peran) {
                                        "ROLE:PETANI" -> stringRole[0]
                                        "ROLE:PENYULUH_PERTANIAN" -> stringRole[1]
                                        "ROLE:MAHASISWA" -> stringRole[2]
                                        "ROLE:DOSEN_MITRA_PTN_LAIN" -> stringRole[3]
                                        "ROLE:DOSEN_IPB" -> stringRole[4]
                                        "ROLE:PUSAT_STUDI_PENELITIAN" -> stringRole[6]
                                        "ROLE:FAKULTAS_DAN_DEPARTEMEN" -> stringRole[5]
                                        "ROLE:SUB_ADMIN" -> stringRole[7]
                                        "ROLE:SUPER_ADMIN" -> stringRole[8]
                                        else -> "Peran belum diatur"
                                    }
                                var user = User(
                                    item.id,
                                    item.username,
                                    item.email,
                                    kategoriUser,
                                    item.name
                                )
                                if (usersAll.isEmpty()) {
                                    usersAll[item.id.toString()] =
                                        arrayListOf(item.name.toString(), kategoriUser)
                                } else {
                                    usersAll[item.id.toString()] =
                                        arrayListOf(item.name.toString(), kategoriUser)
                                }

                                if (item.id != idUser) {
                                    usersAllEvery.add(user)
                                }
                            }
                            var ruangChatTerakhir: ArrayList<RuangChat> = arrayListOf()

                            //sudah paling akhir, baru urutkan chat dari yang paling terakhir dikirim
                            for (i0 in 0 until messageTerakhirInAllRoom.size) {
                                arrayListId.add(messageTerakhirInAllRoom[i0].id)
                            }
                            //paling terakhir ke paling atas supaya bisa nambah 5 terus.
                            arrayListId.sortDescending()
                            for (i1 in arrayListId.indices) {
                                //sorting message terakhir. yang 0 idny nggak masuk ke sortingan
                                var index =
                                    messageTerakhirInAllRoom.firstOrNull { it.id == arrayListId[i1] }
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
                                notifMuncul[messageTerakhirInAllRoom[i].chat_room_id.toString()] =
                                    false
                                notifMuncul[messageTerakhirInAllRoom[i].chat_room_id.toString()]?.let {
                                    arrayListNotif.add(
                                        it
                                    )
                                }
                                //semua message terakhir

                                var hasilRuangChat = RuangChat(0, "", "", "", 0)
                                var memberdiRuang = ArrayList<ChatRoomMemberData>()
                                if (memberOfPrivateRoomChat[messageTerakhirInAllRoom[i].chat_room_id.toString()] != null) {
                                    memberdiRuang =
                                        memberOfPrivateRoomChat[messageTerakhirInAllRoom[i].chat_room_id.toString()] as ArrayList<ChatRoomMemberData>
                                }
                                Log.d("messageTerakhir", "${messageTerakhirInAllRoom[i]}")
                                Log.d("memberRuang", "$memberdiRuang")
                                var temanNgobrol =
                                    memberdiRuang.firstOrNull { it.user_id != idUser }
                                var userTeman =
                                    usersAllEvery.firstOrNull { it.id == temanNgobrol?.user_id ?: 0 }
                                if (userTeman != null) {
                                    hasilRuangChat = RuangChat(
                                        messageTerakhirInAllRoom[i].chat_room_id,
                                        "${userTeman.username} (${userTeman.name})",
                                        "CHAT_ROOM:PRIVATE",
                                        "${userTeman.kategoriUser}",
                                        0
                                    )
                                }
                                //Toast.makeText(requireActivity(),"${user.name}",Toast.LENGTH_LONG).show()
                                ruangChatTerakhir.add(hasilRuangChat)
                            }

                            Log.d("onGetMessage", "$messageTerakhirInAllRoom")
                            Log.d("ruangChatTerakhir", "$ruangChatTerakhir")
                            adapter.listPrivateChat = ruangChatTerakhir
                            adapter.listMessage = messageTerakhirInAllRoom
                            if (adapter.listOfNotif.isEmpty()) {
                                adapter.listOfNotif = arrayListNotif
                            }
                            binding.progressbarRiwayatPribadi.visibility = View.GONE
                            adapter.notifyDataSetChanged()

                            //loadPrivateChatRoom()


                        }
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    Toast.makeText(
                        requireActivity(),
                        "Silahkan periksa koneksi",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressbarRiwayatPribadi.visibility = View.GONE
                }

            })

    }

    //diganti pake socket.io, jadinya ngambil semua grup yang dia tergabung di dalamnya
    private fun getAllRoomsMember() {
        initialization = false
        val detailUser = sessionManager.userDetails()
        isAdminGrupPrivate.clear()
        jumlahOrangPerGrup.clear()
        messageAllInRoom.clear()
        usersAllEvery.clear()
        //messageTerakhirInAllRoom.clear()
        //messageTerakhirInMember.clear()
        isMemberOfGroup.clear()
        usersAll.clear()
        memberOfPrivateRoomChat.clear()
        //isiPesanDiruangan.clear()

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
                        //sudah dapat response, terima setiap chat room id dengan isi berupa jumlah orang dalam grup
                        //serta terima id chat group yang user yang sedang login adalah admin di dalamny
                        if (chatMembers != null) {
                            if (chatMembers!!.isEmpty()) {
                                //bikin text view yang bilang kalo nggak ada obrolan private
                                Toast.makeText(
                                    requireActivity(),
                                    "Tidak ada group member sama sekali",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //makeRoomChatPrivate()
                            } else {
                                for (chatRoomMember in chatMembers!!) {
                                    //cari user ini termasuk room member yang mana supaya nanti mau dia admin atau siapa pun
                                    //jika isi ruang chatnya ada dua orang baru ambil tampil isapprove untuk pertemanan aja
                                    if (jumlahOrangPerGrup.isEmpty()) {
                                        jumlahOrangPerGrup[chatRoomMember.chat_room_id.toString()] =
                                            1
                                    } else if (jumlahOrangPerGrup.containsKey(chatRoomMember.chat_room_id.toString())) {
                                        var jumlahOrang: Int =
                                            jumlahOrangPerGrup[chatRoomMember.chat_room_id.toString()] as Int
                                        jumlahOrang += 1
                                        jumlahOrangPerGrup[chatRoomMember.chat_room_id.toString()] =
                                            jumlahOrang
                                    } else {
                                        jumlahOrangPerGrup[chatRoomMember.chat_room_id.toString()] =
                                            1
                                    }
                                    //nyari admin masing masing grup
                                    if (isAdminGrupPrivate.isEmpty()) {
                                        if (chatRoomMember.type == "CHAT_MEMBER:ADMIN") {
                                            isAdminGrupPrivate[chatRoomMember.chat_room_id.toString()] =
                                                chatRoomMember.user_id
                                        }
                                    } else if (chatRoomMember.type == "CHAT_MEMBER:ADMIN") {
                                        isAdminGrupPrivate[chatRoomMember.chat_room_id.toString()] =
                                            chatRoomMember.user_id
                                    }
                                    //nyari userid apakah menjadi member
                                    if (chatRoomMember.user_id == idUser) {
                                        isMemberOfGroup.add(chatRoomMember.chat_room_id)
                                    }
                                    //ambil semua member of private
                                    when {
                                        memberOfPrivateRoomChat.isEmpty() -> {
                                            memberOfPrivateRoomChat[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember)
                                        }
                                        memberOfPrivateRoomChat.containsKey(chatRoomMember.chat_room_id.toString()) -> {
                                            var isi =
                                                memberOfPrivateRoomChat[chatRoomMember.chat_room_id.toString()] as ArrayList<ChatRoomMemberData>
                                            isi.add(chatRoomMember)
                                            memberOfPrivateRoomChat[chatRoomMember.chat_room_id.toString()] =
                                                isi
                                        }
                                        else -> {
                                            memberOfPrivateRoomChat[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember)
                                        }
                                    }
                                }
                                //getAllMessagesInGroup()
                                loadAllUser()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentObrolanPribadiBinding.inflate(inflater)
        // Inflate the layout for this fragment
        val view = binding.root
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null

    }

}
