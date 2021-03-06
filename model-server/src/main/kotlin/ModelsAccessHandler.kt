package org.modelix.mps.rest.model.access.server

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.mps.openapi.module.SRepository
import org.jetbrains.mps.openapi.persistence.PersistenceFacade
import org.modelix.mps.rest.model.access.api.Model

class ModelsAccessHandler {
    companion object {
        private var init = false

        lateinit var repo: SRepository

        @JvmStatic
        fun isInitialised(): Boolean {
            return init;
        }

        @JvmStatic
        fun init(repo: SRepository) {
            this.repo = repo
            this.init = true
        }

        @JvmStatic
        fun handle(request: HttpRequest, queryStringDecoder: QueryStringDecoder, channel: Channel) {
            val path = queryStringDecoder.path();
            val method = request.method()

            val array = path.split("/").dropWhile { it.isEmpty() }.dropLastWhile{ it.isEmpty() }

            if (array.size != 3) {
                respondBadRequest(request, channel)
                return;
            }

            val prefix = array[0]
            val modelPath = array[1]
            if (!Model.prefix.equals(prefix) || !Model.path.equals(modelPath)) {
                respondBadRequest(request, channel)
                return;
            }

            val modelId = array[2]

            when (method) {
                HttpMethod.GET -> {
                    var model: Model? = null
                    repo.modelAccess.runReadAction {
                        model = repo.getModel(PersistenceFacade.getInstance().createModelId(modelId))?.serialize()
                    }

                    if (model == null) {
                        respondNotFoundError(request, channel, "Couldn't find model with id $modelId")
                    } else {
                        respondOk(request, channel, Json.encodeToString(model).encodeToByteArray())
                    }
                }
                else -> respondMethodNotAllowed(request, channel)
            }
        }
    }
}