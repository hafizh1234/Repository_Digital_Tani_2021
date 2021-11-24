package id.ac.ipb.mobile.digitani

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import id.ac.ipb.mobile.digitani.adapter.AdapterChatPribadi
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.ActivityRuangChatPribadiBinding
import id.ac.ipb.mobile.digitani.helper.GetPathFromUriHelper
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.*

import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class RuangChatPribadi : AppCompatActivity(), CoroutineScope {

    private var uriImageTerdownload: Uri? = null
    private var mimeType: String? = null
    private var mimeTypeDariDownload: String? = null
    private var sessionManager: SessionManager? = null
    private lateinit var apiService: ApiInterface
    private lateinit var binding: ActivityRuangChatPribadiBinding
    private var ruangChat: RuangChat? = null
    private var refreshList = false
    private lateinit var arrayListOfAllRoomMembers: ArrayList<ChatRoomMemberData>
    private lateinit var rvIsiObrolan: RecyclerView
    private var isOpeningAttachment: Boolean = false
    private var paginationMessage: Int = 0
    private lateinit var memberChatRoomFriend: ChatRoomMemberData
    private lateinit var memberCurrent: ChatRoomMemberData

    private var cameraPermitted: Boolean = false
    private var externalStorage: Boolean = false
    private lateinit var currentPhotoPath: String
    private var mediaUri: Uri? = null
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

    private val takePictureContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                mediaUri.let { fileUri ->
                    val timeStamp: String =
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

                    val relativePath =
                        Environment.DIRECTORY_PICTURES + File.separator + "DigitaniIPB" // save directory

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "DigitaniIPB_${timeStamp}")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        }
                    }
                    mimeType = "image/png"
                    val medUri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    medUri?.let { mUri ->
                        mediaUri = mUri
                        contentResolver.openOutputStream(mUri)?.use { os ->
                            if (fileUri != null) {
                                contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    inputStream.copyTo(os)
                                }
                            }
                        }
                    }
                    binding.cvAttachment.visibility = View.GONE
                    isOpeningAttachment = false
                    binding.cvImageTerpilih.visibility = View.VISIBLE
                    binding.ivFotoTerpilih.setImageURI(mediaUri)
                }
            }
        }

    private lateinit var messagesInARoom: ArrayList<MessageIsi>
    private lateinit var adapter: AdapterChatPribadi
    private lateinit var etIsiPesan: EditText
    private lateinit var arrayListOfUserInRoom: ArrayList<User>
    lateinit var messagesInARoomSimple: ArrayList<OneMessageForOneMember>
    private lateinit var job: Job

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uri ->
                mediaUri = uri
                binding.cvImageTerpilih.visibility = View.VISIBLE
                binding.cvAttachment.visibility = View.GONE
                binding.ivFotoTerpilih.setImageURI(uri)
                isOpeningAttachment = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    currentPhotoPath = GetPathFromUriHelper.getPath(this, uri).toString()
                    val file = File(currentPhotoPath)

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

    //seharusnya gambar sama pathnya di simpan db sqlite (identifier:name asli) biar nggak perlu download terus terusan kalo filenya sudah ada
    //untuk file pribadi simpen pathnya di table lain SQLite supaya nggak perlu download dari internet
    companion object {
        const val EXTRA_RUANG = "Extra_Ruang"
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuangChatPribadiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cvAttachment.visibility = View.GONE
        paginationMessage = 0

        askPermissionCamera()

        val window = this@RuangChatPribadi.window
        val detailUser = sessionManager!!.userDetails()
        val idUserCurrent = detailUser[SessionManager.KEY_ID_USER] as Int
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        window.statusBarColor = ContextCompat.getColor(
            this@RuangChatPribadi,
            R.color.colorPrimaryDark
        )
        rvIsiObrolan = binding.recyclerviewChatLog
        refreshList = true
        var linearLayoutManager = LinearLayoutManager(this)
        rvIsiObrolan.layoutManager = linearLayoutManager
        adapter = AdapterChatPribadi(this, idUserCurrent)
        rvIsiObrolan.adapter = adapter


        adapter.setOnItemClickCallback(object : AdapterChatPribadi.OnItemClickCallback {
            override fun onItemClicked(message: OneMessageForOneMember) {
                unduhGambar(message.image, message.image_name)
            }

            override fun onMessageClicked(
                message: OneMessageForOneMember,
                position: Int
            ): Boolean {

                showDialog("hapusPesan", message, position)
                return true
            }

        })
        binding.btnAttachment.setOnClickListener {
            if (!isOpeningAttachment) {
                binding.cvAttachment.visibility = View.VISIBLE
                isOpeningAttachment = true
            } else {
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.cancelAddGambar.setOnClickListener {
            if (isOpeningAttachment) {
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.sendButtonChatLog.setOnClickListener {
            val isiPesan = etIsiPesan.text.toString()
            if (isiPesan.isEmpty()) {
                binding.etTextPrivateChat.error =
                    "Silahkan masukkan pesan terlebih dahulu."
            } else {
                if (mediaUri != null && mimeType != null) {
                    uploadFile(isiPesan)
                } else {
                    kirimPesan(isiPesan)
                }
            }
        }
        binding.tambahTeman.setOnClickListener {
            //orang lain kita pertama mulai chat dulu dan ditengah perjalanan kita tambah dia sebagai teman
            if (memberChatRoomFriend.type == "CHAT_MEMBER:ADMIN" && !memberCurrent.is_approved && !memberChatRoomFriend.is_approved) {
                approvedUser(memberChatRoomFriend, "langsung")
            } else if (memberCurrent.type == "CHAT_MEMBER:ADMIN" && !memberCurrent.is_approved && !memberChatRoomFriend.is_approved) {
                //kita diajak ngobrol user lain dengan langsung mulai chat terus kita tambahkan dia sebagai teman
                approvedUser(memberCurrent, "userCurrentRegular")
            } else if (memberCurrent.type == "CHAT_MEMBER:ADMIN" && memberCurrent.is_approved && !memberChatRoomFriend.is_approved) {
                //langsung terima pertemanan karena kita jadi admin isapproved true
                approvedUser(memberChatRoomFriend, "langsungTeman")
            }
        }
        binding.abaikan.setOnClickListener {
            binding.linearlayout1.visibility = View.GONE
        }
        binding.tolakPertemananActivity.setOnClickListener {
            showDialog("tolakPertemanan", null, 0)
        }
        binding.btnPickGalleryImage.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.btnCameraAttachment.setOnClickListener {
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


    private fun deleteMessage(messageId: Int, semuaPesanDiruanganSudahDihapus: Boolean) {
        //hanya dipakai jika semua orang sudah menghapus satu pesan yang sama
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMessage("Bearer $token", messageId)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    var result = response.body()?.result
                    if (result == 1) {
                        Toast.makeText(
                            this@RuangChatPribadi,
                            "Berhasil menghapus pesan",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (semuaPesanDiruanganSudahDihapus) {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Semua pesan di obrolan pribadi ini sudah dihapus",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            this@RuangChatPribadi,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatPribadi,
                        "Gagal mengambil data",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
       }

    private fun updateMessage(message: OneMessageForOneMember, position: Int) {
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
                            this@RuangChatPribadi,
                            "Hapus Pesan Berhasil",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    override fun onFailure(call: Call<MessageRespon>, t: Throwable) {
                        Toast.makeText(
                            this@RuangChatPribadi,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

    }

    private fun showDialog(keterangan: String, pesan: OneMessageForOneMember?, position: Int) {
        var dialogMessage: String = ""
        var dialogTitle = ""
        if (keterangan == "tolakPertemanan") {
            dialogMessage =
                "Apakah anda ingin menolak permintaan pertemanan dari user ini? Jika iya, maka seluruh pesan anda dengan user ini akan terhapus."
            dialogTitle = "Penolakan Permintaan Pertemanan"
        } else if (keterangan == "hapusPesan") {
            dialogMessage = "Apakah anda ingin menghapus pesan ini?"
            dialogTitle = "Hapus Pesan"
        }
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (keterangan == "tolakPertemanan") {

                    deleteAllMessageAndAllDataInRoom()

                } else if (keterangan == "hapusPesan") {
                    updateMessage(pesan!!, position)
                }

            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

    private fun deleteAllMessageAndAllDataInRoom() {
        Log.d(RuangChatPribadi::class.java.simpleName, "$messagesInARoomSimple")
        Log.d(RuangChatPribadi::class.java.simpleName, "hapus semua pesan dalam ruangan ini")
        //var sudahDihapusSemua = false
        //messagesInARoomSimple
        if (messagesInARoomSimple.isNotEmpty()) {
            for (message in messagesInARoomSimple) {
                if (message != messagesInARoomSimple[messagesInARoomSimple.size - 1]) {
                    deleteMessage(message.id, false)
                } else {
                    deleteMessage(message.id, true)
                }
                if (message == messagesInARoomSimple[messagesInARoomSimple.size - 1]) {
                    deleteRoomMember(memberChatRoomFriend.id.toString(), false)
                }
            }
        } else {
            deleteRoomMember(memberChatRoomFriend.id.toString(), false)
        }
    }

    private fun approvedUser(chatRoomMember: ChatRoomMemberData, s: String) {
        var detail = sessionManager!!.userDetails()
        var token = detail[SessionManager.KEY_TOKEN_JWT].toString()
        var idUser = detail[SessionManager.KEY_ID_USER]
        var idChatRoom = ""
        var idUserTerpilih: String = ""
        var type = ""
        var idChatRoomMember = 0
        var isApproved = false
        //
        if (chatRoomMember.user_id != idUser && s == "langsung") {
            idUserTerpilih = chatRoomMember.user_id.toString()
            idChatRoomMember = chatRoomMember.id
            isApproved = true
            idChatRoom = chatRoomMember.chat_room_id.toString()
            type = "CHAT_MEMBER:ADMIN"
        } else if (chatRoomMember.user_id == idUser && s == "userCurrentRegular") {
            idUserTerpilih = chatRoomMember.user_id.toString()
            idChatRoomMember = chatRoomMember.id
            isApproved = false
            idChatRoom = chatRoomMember.chat_room_id.toString()
            type = "CHAT_MEMBER:REGULAR"
        } else if (chatRoomMember.user_id != idUser && s == "userFriend") {
            idUserTerpilih = chatRoomMember.user_id.toString()
            idChatRoomMember = chatRoomMember.id
            isApproved = true
            idChatRoom = chatRoomMember.chat_room_id.toString()
            type = "CHAT_MEMBER:ADMIN"
        } else if (chatRoomMember.user_id != idUser && s == "langsungTeman") {
            idUserTerpilih = chatRoomMember.user_id.toString()
            idChatRoomMember = chatRoomMember.id
            isApproved = true
            idChatRoom = chatRoomMember.chat_room_id.toString()
            type = "CHAT_MEMBER:REGULAR"
        }

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.updateMember(
            "Bearer $token",
            idChatRoomMember,
            idUserTerpilih,
            idChatRoom,
            isApproved.toString(),
            type
        )
            .enqueue(object : Callback<ChatMemberPostResponse> {
                override fun onResponse(
                    call: Call<ChatMemberPostResponse>,
                    response: Response<ChatMemberPostResponse>
                ) {
                    if (response.isSuccessful) {
                        when (s) {
                            "langsung" -> {
                                Toast.makeText(
                                    this@RuangChatPribadi,
                                    "teman berhasil ditambahkan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            "userCurrentRegular" -> {
                                approvedUser(memberChatRoomFriend, "userFriend")
                            }
                            "userFriend" -> {
                                Toast.makeText(
                                    this@RuangChatPribadi,
                                    "teman berhasil ditambahkan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            "langsungTeman" -> {
                                Toast.makeText(
                                    this@RuangChatPribadi,
                                    "Permintaan pertemanan berhasil disetujui",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        binding.linearlayout1.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ChatMemberPostResponse>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatPribadi,
                        "Tidak berhasil menambahkan sebagai teman.\nCoba lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun deleteRoom(
        ruangChatId: String
    ) {
        var detailUser = sessionManager!!.userDetails()
        var result: Int = 0
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteRoom("Bearer $token", ruangChatId)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    if (response.isSuccessful) {
                        result = response.body()!!.result
                        if (result == 1) {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Pertemanan ditolak. Dan Ruangan obrolan ini sudah terhapus.",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Gagal menolak permintaan pertemanan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        var intent =
                            Intent(this@RuangChatPribadi, KonsultasiGrupActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        this@RuangChatPribadi.finish()
                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatPribadi,
                        "Gagal menolak permintaan pertemanan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun deleteRoomMember(idMembers: String, roomMemberSemuanyaSudahDidelete: Boolean) {
        var detailUser = sessionManager!!.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMember("Bearer $token", idMembers)
            .enqueue(object : Callback<Delete> {
                //hapus semua pesan terlebih dahulu
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    if (response.isSuccessful) {
                        var status = response.body()!!.result
                        if (status == 1) {

                        } else {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "gagal menolak permintaan pertemanan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        //getAllMessage supaya bisa delete semua pesan diRuangan dulu
                        if (!roomMemberSemuanyaSudahDidelete) {
                            deleteRoomMember(memberCurrent.id.toString(), true)
                        } else {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Semua orang di grup obrolan sudah dikeluarkan.",
                                Toast.LENGTH_LONG
                            ).show()
                            deleteRoom(
                                memberChatRoomFriend.chat_room_id.toString()
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatPribadi,
                        "gagal menolak permintaan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
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
                        this@RuangChatPribadi,
                        "Gagal mendownload gambar",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun writtenToDisk(body: ResponseBody, imageName: String): Boolean {
        //implementasi pemasukkan gambar ke storage mobile
        }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun uploadFile(isiPesan: String?) {
        var file: File? = null
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var memberOfGroup: ChatRoomMemberData? = null
        var idUserSekarang: Int = detailUser[SessionManager.KEY_ID_USER] as Int

        for (isi in arrayListOfAllRoomMembers) {
            if (isi.user_id == idUserSekarang) {
                memberOfGroup = isi
            }
        }
        apiService = ApiClientChatNew.chatRoomApiClient
        if (mediaUri != null) {
            currentPhotoPath = GetPathFromUriHelper.getPath(this, mediaUri!!).toString()
            file = File(currentPhotoPath)
        }
        val requestFile = file!!.asRequestBody(
            mediaUri?.let { contentResolver.getType(it) }!!
                .toMediaTypeOrNull()
        )
        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        var idUser = idUserSekarang.toString().toRequestBody("text/plain".toMediaType())
        var memberId = memberOfGroup!!.id.toString().toRequestBody("text/plain".toMediaType())
        var content = isiPesan?.toRequestBody("text/plain".toMediaType())
        var type = "CHAT_MESSAGE:WITH_IMAGE".toRequestBody("text/plain".toMediaType())

        if (content != null) {
            apiService.postMessageWithImage("Bearer $token", body, memberId, content, type)
                .enqueue(object : Callback<MessageRespon> {
                    override fun onResponse(
                        call: Call<MessageRespon>,
                        response: Response<MessageRespon>
                    ) {
                        if (response.isSuccessful) {
                            var message: Message = response.body()!!.chat_message
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Pesan dengan gambar berhasil dikirim",
                                Toast.LENGTH_SHORT
                            ).show()
                            getAllMembersInGroup()

                        } else {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Gambar terlalu besar.\nSilahkan kirim gambar lain max 1MB",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onFailure(call: Call<MessageRespon>, t: Throwable) {
                        Toast.makeText(
                            this@RuangChatPribadi,
                            "Gagal Mengirim Pesan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
        }
    }

    private fun askPermissionCamera() {
        val stringOfPerm = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermission.launch(stringOfPerm)
    }

    private fun kirimPesan(isiPesan: String) {
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idCurrent = detailUser[SessionManager.KEY_ID_USER] as Int
        var memberOfGroup: ChatRoomMemberData? = null

        for (isi in arrayListOfAllRoomMembers) {
            if (isi.user_id == idCurrent) {
                memberOfGroup = isi
            }
        }
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.postMessages(
            "Bearer $token",
            memberOfGroup!!.id.toString(),
            isiPesan,
            "CHAT_MESSAGE:ONLY_TEXT"
        )
            .enqueue(object : Callback<MessageRespon> {
                override fun onResponse(
                    call: Call<MessageRespon>,
                    response: Response<MessageRespon>
                ) {
                    if (response.isSuccessful) {
                        var message: Message = response.body()?.chat_message!!
                        Toast.makeText(
                            this@RuangChatPribadi,
                            "Pesan berhasil dikirim",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etTextPrivateChat.text = null

                        getAllMembersInGroup()

                    }
                }

                override fun onFailure(call: Call<MessageRespon>, t: Throwable) {

                }

            })
    }

    //EasyImagePicker tidak digunakan karena mengharuskan memakai onActivityResult yang sudah deprecated
// easyImage tidak dipakai karena tidak bisa mencari ukuran file
    @Throws(IOException::class)
    fun takePic() {
        try {
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val folder = File(filesDir, "DigitaniIPB")
            val file = File(folder, "Digitani_${timeStamp}")
//mimeType = getMimeType(file)
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

    //untuk mulai ngechat, identifikasi dulu usernya di grup chat pribadi ini. Lalu simpan chat room id masing masing
    fun getAllMembersInGroup() {
        arrayListOfAllRoomMembers.clear()
        val detailUser = sessionManager?.userDetails()
        val token = detailUser?.get(SessionManager.KEY_TOKEN_JWT).toString()
        var idUser = detailUser?.get(SessionManager.KEY_ID_USER) as Int

        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getAllMembersInOneRoom("Bearer $token", ruangChat?.id.toString())
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        //initializationMessage = true
                        //afterTerimaPesan = false
                        val isi = response.body()

                        if (isi != null) {
                            arrayListOfAllRoomMembers.addAll(isi.chat_members)
                            //Toast.makeText(this@RuangChatPribadi,"$arrayListOfIsiRoomChat",Toast.LENGTH_SHORT).show()
                        }
                        memberCurrent =
                            arrayListOfAllRoomMembers.firstOrNull { it.user_id == idUser && it.chat_room_id == ruangChat!!.id }!!
                        memberChatRoomFriend =
                            arrayListOfAllRoomMembers.firstOrNull { it.user_id != idUser && it.chat_room_id == ruangChat!!.id }!!
                        if (memberChatRoomFriend!!.is_approved && memberChatRoomFriend.type == "CHAT_MEMBER:ADMIN") {
                            if (memberCurrent!!.is_approved) {
                                binding.linearlayout1.visibility = View.GONE
                                //sudah berteman dan makanya hilang semua tulisan anda belum berteman...
                            } else {
                                //teman dalam satu ruangan yang sama teman itu sebagai admin dan isApproved
                                //userCurrent sendiri belom diapproved sama user yang lain sebagai teman (belum diterima)
                                binding.linearlayout1.visibility = View.VISIBLE
                                binding.tambahTeman.visibility = View.GONE
                                binding.tolakPertemananActivity.visibility = View.GONE
                                binding.abaikan.visibility = View.GONE

                                binding.tvPenjelasan.text =
                                    "Permintaan pertemanan anda belum disetujui."
                                binding.tvPenjelasan.textAlignment =
                                    TextView.TEXT_ALIGNMENT_CENTER
                                MainScope().launch {
                                    delay(2000L)
                                    binding.linearlayout1.visibility = View.GONE
                                }
                            }
                        } else if (memberCurrent!!.is_approved && memberCurrent.type == "CHAT_MEMBER:ADMIN") {
                            //kita ditambahkan sebagai teman oleh user lain sehingga kita dapat menerima permintaan pertemanan dan kita sebagai admin dan approved=true
                            if (memberChatRoomFriend.is_approved == false) {
                                binding.linearlayout1.visibility = View.VISIBLE
                                binding.tvPenjelasan.text =
                                    "Anda telah ditambahkan sebagai teman oleh user ini."
                                binding.tambahTeman.text = "TERIMA PERTEMANAN"
                                binding.tolakPertemananActivity.text = "TOLAK PERTEMANAN"
                                binding.abaikan.text = "ABAIKAN INFORMASI"
                            }
                        } else if (!memberChatRoomFriend.is_approved && !memberCurrent.is_approved) {
                            //orang lain hanya memulai chat dengan kita, kita hanya sebagai admin yang belom approved
                            binding.linearlayout1.visibility = View.VISIBLE
                            binding.tvPenjelasan.text =
                                "Anda sedang berbicara dengan user yang bukan dari kontak pertemanan anda."
                            binding.tambahTeman.text = "TAMBAH TEMAN"
                            binding.tolakPertemananActivity.visibility = View.GONE
                            binding.abaikan.text = "ABAIKAN "
                        }
                        getAllMessagesInGroup()
                    }
                }

                override fun onFailure(
                    call: Call<ChatRoomMemberResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@RuangChatPribadi,
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

    }

    private fun getAllMessagesInGroup() {
      
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idUserCurrent = detailUser[SessionManager.KEY_ID_USER].toString()
        var messagesInARoomUser: OneMessageForOneMember
        var timeHasil: ArrayList<String> = arrayListOf()
        var messageSentCreated: String = ""
        var date: Date = Date()
        var arrayListTgl: ArrayList<Date> = arrayListOf()

        var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        messagesInARoomSimple.clear()
        apiService = ApiClientChatNew.chatRoomApiClient
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
                            binding.sendButtonChatLog.visibility = View.VISIBLE
                            messagesInARoom = response.body()!!.chat_messages
                            if (messagesInARoom.isNotEmpty()) {
                                for (item in messagesInARoom) {
                                    var roomMember =
                                        arrayListOfAllRoomMembers.firstOrNull { it.id == item.chat_member_id }
                                    if (roomMember != null) {
                                        if (roomMember.chat_room_id == ruangChat!!.id) {
                                            var messageResult = ""
                                            var messagePenjelasan = ""
                                            var messageContent = item.content.split("#$%")
                                            var tampil = true
                                            if (messageContent.size > 1) {
                                                for (messageIsi in messageContent) {
                                                    if (messageIsi != messageContent[messageContent.size - 1]) {
                                                        messageResult =
                                                            "$messageResult$messageIsi"
                                                    } else {
                                                        messagePenjelasan = messageIsi
                                                    }
                                                }
                                            } else {
                                                messageResult = item.content
                                            }
                                            if (messagePenjelasan != "") {
                                                var isiSatuan = messagePenjelasan.split(",")
                                                for (satuanPenjelasan in isiSatuan) {
                                                    var keterangan = satuanPenjelasan.split(":")

                                                    if (idUserCurrent == keterangan[0]) {
                                                        tampil = false

                                                    }
                                                }
                                            }

                                            var stringDate = item.created_at
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
                                            arrayListTgl.add(date)
                                            if (item.type == "CHAT_MESSAGE:ONLY_TEXT") {
                                                messagesInARoomUser = OneMessageForOneMember(
                                                    item.id,
                                                    item.type,
                                                    messageResult,
                                                    item.chat_member_id,
                                                    item.deleted_by_str,
                                                    date,
                                                    "",
                                                    "",
                                                    roomMember.chat_room_id,
                                                    roomMember.user_id,
                                                    !tampil,
                                                    false
                                                )
                                            } else {
                                                var tombolUnduhDitampilkan = true

                                                var imageOriginalName =
                                                    item.image!!.original_name
                                                val folder = File(filesDir, "DigitaniIPB")
                                                val file = File(folder, imageOriginalName)
                                                //Toast.makeText(this@RuangChatPribadi,"${item.image?.original_name}",Toast.LENGTH_SHORT).show()
                                                if (!folder.exists()) {
                                                    folder.mkdirs()
                                                }
                                                if (file.exists()) {
                                                    tombolUnduhDitampilkan = false
                                                    //Toast.makeText(this@RuangChatGroup, "File Masih ada di penyimpanan", Toast.LENGTH_SHORT).show()
                                                }
                                                messagesInARoomUser = OneMessageForOneMember(
                                                    item.id,
                                                    item.type,
                                                    messageResult,
                                                    item.chat_member_id,
                                                    item.deleted_by_str,
                                                    date,
                                                    item.image!!.original_name,
                                                    item.image!!.url,
                                                    roomMember.chat_room_id,
                                                    roomMember.user_id,
                                                    !tampil,
                                                    !tombolUnduhDitampilkan
                                                )
                                            }
                                            //pengecekan apakah dia ada di storage atau nggak sama kayak current atau user lain
                                            /*
                                                                id->true/false
                                                               */
                                            messagesInARoomSimple.add(messagesInARoomUser)
                                            //Toast.makeText(this@RuangChatPribadi, "${item.content}",Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            
                            adapter.listMessages = messagesInARoomSimple
                            adapter.notifyDataSetChanged()
                            getAllUser()
                        }


                    }
                }

                override fun onFailure(call: Call<GetChatMessages>, t: Throwable) {

                }
            })
    }

    //ini untuk mendapatkan dan untuk menuliskan nama sama foto semua user(nanti kalo dibuat ke setiap pesan yang dikirim, ditaro di atasnya supaya tau siapa yang ngirim pesan)
    private fun getAllUser() {
        arrayListOfUserInRoom.clear()
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idCurrent = detailUser[SessionManager.KEY_ID_USER] as Int
        var users: ArrayList<UserLoginData>
        var kategori: UserCategory? = null
        var peran: String = ""
        var user: User? = null
        var arrayListUser: ArrayList<User> = arrayListOf()

        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        users = response.body()!!.users

                        if (users.isEmpty()) {
                            Toast.makeText(
                                this@RuangChatPribadi,
                                "Tidak ada user",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            binding.sendButtonChatLog.visibility = View.VISIBLE
                            for (item in users) {
                                if (!item.roles?.isEmpty()!!) {
                                    kategori = item.roles!![0]
                                    peran = kategori!!.name.toString()
                                } else {
                                    peran = "Peran Not Set Yet"
                                }
                                var kategoriUser = ""
                                val stringRole =
                                    resources.getStringArray(R.array.RoleAll)
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
                                        else -> "Peran Belum Diatur"
                                    }

                                //ambil idInRoomChat ada 2 aja ->User

                                if (item.id == memberCurrent.user_id || item.id == memberChatRoomFriend.user_id) {
                                    user = User(
                                        item.id,
                                        item.username,
                                        item.email,
                                        kategoriUser,
                                        item.name
                                    )
                                    //Toast.makeText(this@RuangChatPribadi,"$user",Toast.LENGTH_SHORT).show()
                                    arrayListOfUserInRoom.add(user!!)
                                }


                            }
                            adapter.arrayListOfUserInRoom = arrayListOfUserInRoom
                            adapter.notifyDataSetChanged()
                        }

                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {

                }

            })
    }
}
