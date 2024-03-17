package show.taps

sealed class BindServiceState{
    data object Connecting : BindServiceState()
    data object Success : BindServiceState()
    data object Fail : BindServiceState()

}
