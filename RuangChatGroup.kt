package id.ac.ipb.mobile.digitani

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import id.ac.ipb.mobile.digitani.adapter.AdapterChatGroup
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.ActivityRuangChatGroupBinding
import id.ac.ipb.mobile.digitani.helper.GetPathFromUriHelper
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.*
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class RuangChatGroup : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiInterface

    private lateinit var isiPesan: String

    private lateinit var mSocket: Socket
    private lateinit var currentPhotoPath: String
    private var mediaUri: Uri? = null
    private var isApproved: Boolean = false
    private var cameraPermitted: Boolean = false
    private var externalStorage: Boolean = false
    private var refreshList = false
    private lateinit var memberCurrent: ChatRoomMemberData
    private var mimeType: String? = null
    private var isOpeningAttachment: Boolean = false

    private lateinit var adapter: AdapterChatGroup
    private lateinit var etIsiPesan: EditText
    private lateinit var arrayListOfUserInRoom: ArrayList<User>
    lateinit var messagesInARoomSimple: ArrayList<OneMessageForOneMember>
    private lateinit var job: Job
    private lateinit var binding: ActivityRuangChatGroupBinding

    private var ruangChat: RuangChat? = null
    private var requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            it.entries.forEach {
                if (it.key == "android.permission.CAMERA") {
                    if (it.value == false) {
                        Toast.makeText(
                            this,
                            "Kamera harus diizinkan untuk menggunakan kamera",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        cameraPermitted = true
                    }
                } else if (it.key == "android.permission.WRITE_EXTERNAL_STORAGE") {
                    if (it.value == false) {
                        Toast.makeText(
                            this,
                            "Penyimpanan harus diizinkan untuk menyimpan hasil kamera",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        externalStorage = true
                    }
                }

            }

        }

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uri ->
                //ambil foti dari galeri dengan mengambil foto berekstensi png dan jpeg/jpg
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    currentPhotoPath = GetPathFromUriHelper.getPath(this, uri).toString()

                    val string = currentPhotoPath.split(".")
                    for (isi in string) {
                        if (isi == string[string.size - 1]) {
                            if (isi == "png") {
                                mimeType = "image/png"
                            } else if (isi == "jpg" || isi == "jpeg") {
                                mimeType = "image/jpeg"
                            }
                        }
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Maaf versi android yang dibutuhkan minimal adalah versi Kitkat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    private val takePictureContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                //ambil foto dengan kamera, namakan file tersebut dengan nama "ddMMyyyy_HHmmss"
                // dan masukkan ke dalam storage penyimpanan melalui ContentResolver
            }
        }

    companion object {
        const val EXTRA_RUANG = "Extra Ruang"
        const val Is_Approved = "Is_Approved"
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuangChatGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isApproved = false
        askPermissionCamera()
        binding.progressbarRuangGrup.visibility = View.VISIBLE
        isiPesan = ""
        val window = this@RuangChatGroup.window
        window.statusBarColor = ContextCompat.getColor(
            this@RuangChatGroup,
            R.color.colorPrimaryDark
        )

        try {
            //koneksi ke socket.io harus pake JWT
            //This address is the way you can connect to localhost with AVD(Android Virtual Device)
            var opts: IO.Options = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            opts.query = "token=$token"

            mSocket = IO.socket("URL_DIGITANI", opts)
            mSocket.io().on(Manager.EVENT_RECONNECT_ATTEMPT) { opts.query = "token=$token" }

            //Log.d(RuangChatPribadi::class.java.simpleName, mSocket.id())
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(RuangChatPribadi::class.java.simpleName, "Failed to connect")
        }
        mSocket.connect()
        ruangChat = intent.getParcelableExtra<RuangChat>(EXTRA_RUANG)
        isApproved = intent.getBooleanExtra(Is_Approved, false)

        if (ruangChat != null) {
            binding.sendButtonChatLog.visibility = View.VISIBLE
            mSocket.on(Socket.EVENT_CONNECT, onConnect)
            binding.tvGroupName.text = ruangChat!!.name
            binding.tvGroupDescription.text = ruangChat!!.description
        }

        //Taruh semua listener dalam onCreate()
        mSocket.on("user:connect", onConnectUser)
        mSocket.on("user:room:join", onJoiningRoom)
        mSocket.on("message:fetch", onGetMessage)
        mSocket.on("message:send", onMessageSend)
        binding.btnAttachment.setOnClickListener {
            if (!isOpeningAttachment) {
                binding.cvAttachment.visibility = View.VISIBLE
                isOpeningAttachment = true
            } else {
                //tombol seharusnya nimpa yang lain tapi ternyata image malah ketutupan
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.cancelAddGambarToGroup.setOnClickListener {
            if (isOpeningAttachment) {
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.btnPickGalleryImageGroup.setOnClickListener {
            pickImages.launch("image/*")
        }
        binding.recyclerviewChatLog.layoutManager = linearLayoutManager
        adapter = AdapterChatGroup(this, idUserCurrent)
        binding.recyclerviewChatLog.adapter = adapter
        adapter.setOnItemClickCallback(object : AdapterChatGroup.OnItemClickCallback {
            override fun onItemClicked(message: OneMessageForOneMember) {
                unduhGambar(message.image, message.image_name)
            }

            override fun onItemLongClicked(message: OneMessageForOneMember): Boolean {
                showDialogFrag("hapusPesan", message)
                return true
            }

        })
        binding.sendButtonChatLog.setOnClickListener {
            isiPesan = etIsiPesan.text.toString()
            if (isiPesan.isEmpty()) {
                binding.etTextDraft.error =
                    "Silahkan masukkan pesan terlebih dahulu."
            } else {
                if (isApproved) {
                    if (mediaUri != null) {
                        sendSocketImage(isiPesan)
                        //uploadFile(isiPesan)
                    } else {
                        var jsonObject = JSONObject()

                        try {
                            jsonObject.put("chat_member_id", memberOfGroup.id)
                            jsonObject.put("content", isiPesan)
                            jsonObject.put("chat_room_id", memberOfGroup.chat_room_id)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
                        }
                        //kirimPesan(isiPesan)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Anda Belum Diterima masuk ke dalam grup ini. Sehingga anda tidak dapat mengirim atau melihat pesan apapun.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.btnCameraAttachmentGroup.setOnClickListener {
            if (cameraPermitted && externalStorage) {
                takePic()
            }
        }
        binding.cancelImage.setOnClickListener {
            binding.cvImageTerpilih.visibility = View.GONE
            mediaUri = null
            mimeType = null
        }

    }

    private fun sendSocketImage(isiPesan: String) {
        var detailUser = sessionManager?.userDetails()
        var idUserCurrent = detailUser?.get(SessionManager.KEY_ID_USER) as Int
        var tidakBisaDikirim = false
        var file: File? = null
        var bytes: ByteArray?
        binding.progressbarRuangGrup.visibility = View.VISIBLE
        var bytesRead: Int = 0
        var metaData = JSONObject()
        var jsonObject = JSONObject()
        var lastModifiedDate: String = ""
        var byteArrayOutput = ByteArrayOutputStream()
        if (mediaUri != null) {
            currentPhotoPath = GetPathFromUriHelper.getPath(this, mediaUri!!).toString()
            file = File(currentPhotoPath)
        }
        var inputStream: InputStream = FileInputStream(file)
        var buffer = ByteArray(((file?.length() ?: 0) + 100).toInt())
        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutput.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
        }

        bytes = byteArrayOutput.toByteArray()
        var stringImage = Base64.encodeToString(bytes, Base64.NO_WRAP)
        var kumpulanKeterangan = currentPhotoPath.split(".")
        var mimeTypeAkhir = kumpulanKeterangan[kumpulanKeterangan.size - 1]

        val encodedString: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.util.Base64.getEncoder().withoutPadding().encodeToString(bytes)
        } else {
            ""
        }
        var ekstension = ""
        Log.d("testing 1", "$encodedString")
        var stringImageAkhir: String = ""
        if (mimeTypeAkhir == "png") {
            ekstension = "png"
            stringImageAkhir = "data:image/png;base64,$stringImage"
        } else if (mimeTypeAkhir == "jpeg" || mimeTypeAkhir == "jpg") {
            stringImageAkhir = "data:image/jpeg;base64,$stringImage"
            ekstension = "jpeg"
        } else {
            ekstension = "$mimeTypeAkhir"
            stringImageAkhir = "data:image/$mimeTypeAkhir;base64,$stringImage"
        }
        var stringImagedepan: String = ""
        for (i in 0 until 100) {
            stringImagedepan = "$stringImagedepan${stringImage?.get(i)}"
        }
        if (!tidakBisaDikirim) {
            Log.d("Testing Gambar", "${bytes.size},${stringImageAkhir}")

            metaData.put("contentType", "image/$ekstension")
            if (file != null) {
                metaData.put("filename", file.name)
            } else {
                metaData.put("filename", "")
            }
            metaData.put("knownLength", bytes.size)
            if (file != null) {
                metaData.put("lastModified", file.lastModified())
            } else {
                metaData.put("lastModified", 0)
            }
            metaData.put("lastModifiedDate", lastModifiedDate)

            try {
                jsonObject.put("chat_member_id", memberOfGroup.id)
                jsonObject.put("content", isiPesan)
                jsonObject.put("chat_room_id", memberOfGroup.chat_room_id)
                jsonObject.put("image", stringImageAkhir)
                jsonObject.put("meta", metaData)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
            }

            mSocket.emit("message:send", jsonObject)
        }
    }

    var onConnect = Emitter.Listener {

        var jsonObject = JSONObject()
        var detailUser = sessionManager?.userDetails()
        var userId = detailUser?.get(SessionManager.KEY_ID_USER) as Int
        try {
            jsonObject.put("user_id", userId)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(RuangChatPribadi::class.java.simpleName, "$e")
        }
        mSocket.emit("user:connect", jsonObject)

    }
    private var onConnectUser = Emitter.Listener {
        if (it[0] is Boolean && it[0] == false) {
            runOnUiThread(Runnable {
                var clientId = it as Array<*>
                for (i in clientId) {
                    Toast.makeText(
                        this@RuangChatGroup,
                        "user gagal terkoneksi dengan server.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        } else {
            var jsonObject = JSONObject()
            try {
                ruangChat?.let { it1 -> jsonObject.put("chat_room_id", it1.id) }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(RuangChatPribadi::class.java.simpleName, "$e")
            }
            mSocket.emit("user:room:join", jsonObject)
        }
    }
    private var onJoiningRoom = Emitter.Listener {
        getAllMembersInGroup()
    }

    private var onGetMessage = Emitter.Listener {
        messagesInARoomSimple.clear()
        var date = Date()
        var detailUser = sessionManager?.userDetails()
        var pesanSatuan = JSONObject()
        var jsonArray: JSONArray = JSONArray()
        if (it[0] is JSONArray) {
            jsonArray = it[0] as JSONArray
        }
        for (i in 0 until jsonArray.length()) {
            //langsung dapat semua pesan dalam ruangan yang dimasukki
            pesanSatuan = jsonArray.getJSONObject(i)
            var chatMember = pesanSatuan.getJSONObject("chat_member")
            var image = pesanSatuan.get("image")
            var urlImage: String? = ""
            var imageUrlFix: String = ""
            var imageName: String? = ""
            if (image is JSONObject && image != null) {
                imageName = image.getString("original_name")
                urlImage = image.getString("path")
                if (urlImage is String && urlImage != null) {
                    urlImage = "URL"
                }
            }
            var deletedByUser: String? = pesanSatuan.getString("deleted_by_str")
            var createdAt: String = pesanSatuan.getString("created_at")

            var content = pesanSatuan.getString("content")
            var idMessage = pesanSatuan.getInt("id")
            var userIdSender = chatMember.getInt("user_id")
            var chatMemberId = pesanSatuan.getInt("chat_member_id")
            //olah data yang diberikan webserver
            if (initializationMessage) {
                //implementasi pesan yang masuk dengan memulai mengisi recyclerview dari yang paling bawah
            } else if (terimaPesan) {
                //update recyclerview sehingga, pesan yang dikirimkan orang lain, menjadi pesan paling bawah sepertin chattng pada ruangan obrolan umumnya
            } else if (deleteMessage) {
                //update recyclerview, tapi karena menggunakan http request delete dan refresh chat hanya terjadi di hp client yang melakukan delete        }
            }
            runOnUiThread(Runnable {
                adapter.listMessages = messagesTerakhir
                adapter.notifyDataSetChanged()
                if (refreshList && messagesInARoomSimple.size > 0) {
                    refreshList = false
                    binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
                }
            })
            getAllUser()
        }
    }

    var onMessageSend = Emitter.Listener {
        var jsonObject = JSONObject()
        var jsonObjectRuang = JSONObject()

        if (it[0] is JSONObject) {
            for (i in it) {
                jsonObject = it[0] as JSONObject
                Log.d("hasil kiriman broadcas", "${it[0]}")
            }
        } else {

        }
        try {
            jsonObjectRuang.put("chat_room_id", ruangChat?.id.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
        }
        runOnUiThread(Runnable {
            binding.progressbarRuangGrup.visibility = View.VISIBLE
            mediaUri = null
            mimeType = null
        })
        mSocket.emit("message:fetch", jsonObjectRuang)

    }

    private fun updateMessage(message: OneMessageForOneMember) {
        //menghapus pesan dengan cara mengupdate atribut pesan, yaitu deleted_by_str sehingga pesan tetap ada dalam database dan dapat dilihat oleh anggota grup lain.
        //var harusDiDelete adalah variabel yang menyatakan bahwa semua anggota grup sudah menghapus salah satu pesan yang sama di dalam grup.
        //jika false maka masih ada anggota dalam grup ini yang belum menghapus pesan tersbut. Namun, jika true maka semua anggoat grup sudah menghapus satu pesan yang sama.
        if (!harusDiDelete) {
            apiService = ApiClientChatNew.chatRoomApiClient
            apiService.updateMessage(
                "Bearer $token",
                message.id,
                message.chat_member_id.toString(),
                message.content,
                keteranganPalingAkhir,
                message.type
            )
                .enqueue(object : Callback<MessageRespon> {
                    override fun onResponse(
                        call: Call<MessageRespon>,
                        response: Response<MessageRespon>
                    ) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Hapus Pesan Berhasil",
                            Toast.LENGTH_SHORT
                        ).show()

                        var jsonObject = JSONObject()

                        try {
                            jsonObject.put("chat_room_id", ruangChat?.id.toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
                        }
                        mSocket.emit("message:fetch", jsonObject)

                    }

                    override fun onFailure(call: Call<MessageRespon>, t: Throwable) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun deleteMessage(messageId: Int, semuaPesanDiruanganSudahDihapus: Boolean) {

        var detailUser = sessionManager?.userDetails()
        var token = detailUser?.get(SessionManager.KEY_TOKEN_JWT)?.toString()
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMessage("Bearer $token", messageId)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    var result = response.body()?.result
                    if (result == 1) {
                        if (semuaPesanDiruanganSudahDihapus) {
                            Toast.makeText(
                                this@RuangChatGroup,
                                "Semua pesan di obrolan Group ini sudah dihapus",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            this@RuangChatGroup,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    var jsonObject = JSONObject()

                    try {
                        jsonObject.put("chat_room_id", ruangChat?.id.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
                    }
                    mSocket.emit("message:fetch", jsonObject)

                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(this@RuangChatGroup, "Gagal mengambil data", Toast.LENGTH_LONG)
                        .show()
                }
            })
    }

    private fun showDialogFrag(keterangan: String, pesan: OneMessageForOneMember?) {
        var dialogMessage: String = ""
        var dialogTitle: String = ""
        if (keterangan == "hapusPesan") {
            dialogMessage = "Apakah anda ingin menghapus pesan ini?"
            dialogTitle = "Hapus Pesan"
        }
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (keterangan == "hapusPesan") {
                    updateMessage(pesan!!)
                }

            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }


    //EasyImagePicker tidak digunakan karena mengharuskan memakai onActivityResult yang sudah deprecated
    // easyImage tidak dipakai karena tidak bisa mencari ukuran file
    @Throws(IOException::class)
    fun takePic() {
        try {
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val folder = File(filesDir, "DigitaniIPB")
            val file = File(folder, "JPEG_${timeStamp}")

            if (!folder.exists()) {
                folder.mkdirs()
            }
            if (file.createNewFile() || file.exists()) {
                mediaUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

                takePictureContract.launch(mediaUri)
            } else {
                null
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun unduhGambar(image: String, imageName: String) {
        apiService = ApiClient.getGambarInMessage
        apiService.getGambarInPesan("$image")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        var hasilImage = response.body()
                        //jika response success maka tulis ke database file baru storage aja
                        if (hasilImage != null) {
                            writtenToDisk(hasilImage, imageName)
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatGroup,
                        "Gagal mendownload gambar",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun askPermissionCamera() {
        val stringOfPerm = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermission.launch(stringOfPerm)
    }

    private fun writtenToDisk(body: ResponseBody, imageName: String): Boolean {
        //implementasi penulisan file di dalam storage
        return true
    }

    private fun getAllMembersInGroup() {

        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getAllMembersInOneRoom("Bearer $token", ruangChat?.id.toString())
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        var chatRoomMember = response.body()?.chat_members
                        //maksimal 500 member
                        if (!memberCurrent.is_approved) {
                            binding.linearlayout2.visibility = View.VISIBLE
                            binding.batalkanGabung.visibility = View.GONE
                            binding.abaikan.visibility = View.GONE
                            binding.tvPenjelasan.text =
                                "Anda belum disetujui bergabung ke ruang chat ini, sehingga anda tidak bisa melihat ataupun mengirim pesan di grup ini. "
                            MainScope().launch {
                                delay(2000L)
                                binding.linearlayout2.visibility = View.GONE
                            }
                            binding.progressbarRuangGrup.visibility = View.GONE
                        } else {
                            var jsonObject = JSONObject()

                            try {
                                jsonObject.put("chat_room_id", ruangChat?.id.toString())
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e(this@RuangChatGroup::class.java.simpleName, "$e")
                            }
                            mSocket.emit("message:fetch", jsonObject)
                        }
                    }
                }


                override fun onFailure(call: Call<ChatRoomMemberResponse>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatGroup,
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

    }

    private fun getAllUser() {
        val detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        arrayListOfUserInRoom.clear()
        var idCurrent = detailUser[SessionManager.KEY_ID_USER] as Int
        var users: ArrayList<UserLoginData>
        var kategori: UserCategory?
        var peran: String
        var user: User?
        var arrayListUser: ArrayList<User> = arrayListOf()

        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        users = response.body()!!.users

                        if (users.isEmpty()) {
                            Toast.makeText(
                                this@RuangChatGroup,
                                "Tidak ada user",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.sendButtonChatLog.visibility = View.VISIBLE
                            for (item in users) {
                                //tambahkan user yang ada di grup ini ke dalam recyclerview
                            }
                        }
                        adapter.arrayListOfUserInRoom = arrayListOfUserInRoom
                        adapter.notifyDataSetChanged()
                    }

                }


                override fun onFailure(call: Call<Users>, t: Throwable) {

                }

            })
    }
}