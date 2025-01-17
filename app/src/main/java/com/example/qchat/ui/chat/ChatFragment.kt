package com.example.qchat.ui.chat

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qchat.R
import com.example.qchat.adapter.ChatAdapter
import com.example.qchat.databinding.ChatFragmentBinding
import com.example.qchat.model.ChatMessage
import com.example.qchat.model.User
import com.example.qchat.ui.main.MainActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.decodeToBitmap
import dagger.hilt.android.AndroidEntryPoint
import com.example.qchat.fragments.PopMenuFragment
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.chat_fragment) {

    private lateinit var mainActivity: MainActivity
    private lateinit var binding: ChatFragmentBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var user: User
    private lateinit var chatAdapter: ChatAdapter

    @Inject
    lateinit var pref: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity.setBottomNavigationVisibility(View.GONE)
        return inflater.inflate(R.layout.chat_fragment, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity.setBottomNavigationVisibility(View.VISIBLE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ChatFragmentBinding.bind(view)

        getArgument()
        setClickListener()
        setRecyclerview()

        binding.tvName.text = user.name
        binding.ivUserImage.setImageBitmap(user.image?.decodeToBitmap())

        observeChat()

        val ivAdd: ImageView = binding.ivAdd
        ivAdd.setOnClickListener {
            val popUpFragment = PopMenuFragment()
            popUpFragment.show(parentFragmentManager, "PopUpFragment")
        }

        // Set OnClickListener for the username to navigate to the profile
        binding.tvName.setOnClickListener {
            // Pass the entire User object to ProfileFragment
            val action = ChatFragmentDirections.actionChatFragmentToProfileFragment(user)
            findNavController().navigate(action)
        }
    }

    private fun getArgument() {
        arguments?.let {
            user = ChatFragmentArgs.fromBundle(it).user
        }
    }

    private fun observeChat() {
        viewModel.eventListener(user.id, object : ChatObserver {
            override fun observeChat(newChat: List<ChatMessage>) {
                if (newChat.isNotEmpty()) {
                    chatAdapter.addMessage(newChat, binding.rvChat)
                }
                binding.pb.visibility = View.GONE
                viewModel.conversionId.isEmpty().let {
                    if (chatAdapter.getMessageSize() != 0) {
                        viewModel.checkForConversation(user.id)
                    }
                }
            }
        })
    }

    private fun setClickListener() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.ivSend.setOnClickListener {
            if (binding.etMessage.text.isNullOrBlank()) return@setOnClickListener
            viewModel.sendMessage(binding.etMessage.text.trim().toString(), user)
            binding.etMessage.text.clear()
        }
    }

    private fun setRecyclerview() {
        chatAdapter = ChatAdapter(pref.getString(Constant.KEY_USER_ID, null).toString(), emptyList())
        user.image?.let {
            chatAdapter.setProfileImage(it.decodeToBitmap())
        }
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    interface ChatObserver {
        fun observeChat(newChat: List<ChatMessage>)
    }

    override fun onResume() {
        super.onResume()
        viewModel.listenerAvailabilityOfReceiver(user.id) { availability, fcm, profileImage ->
            binding.tvAvailability.isVisible = availability
            user.token = fcm
            if (user.image.isNullOrEmpty()) {
                user.image = profileImage
                binding.ivUserImage.setImageBitmap(user.image?.decodeToBitmap())
                chatAdapter.setProfileImage(user.image?.decodeToBitmap()!!)
            }
        }
    }
}
