private class Item
private class Token
private class Post

private suspend fun postItem(item: Item) {
    val token = requestToken()
    val post = createPost(token, item)
    processPost(post)
}

private suspend fun requestToken(): Token = Token()
private suspend fun createPost(token: Token, item: Item): Post = Post()
private fun processPost(post: Post) {
    println("processPost")
}