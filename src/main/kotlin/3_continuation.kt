class Item
class Token
class Post

suspend fun postItem(item: Item) {
    val token = requestToken()
    val post = createPost(token, item)
    processPost(post)
}

suspend fun requestToken(): Token = Token()
suspend fun createPost(token: Token, item: Item): Post = Post()
fun processPost(post: Post) {
    println("processPost")
}