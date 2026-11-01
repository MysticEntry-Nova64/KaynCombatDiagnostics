package kayn.diagnostics;

import org.lwjgl.opengl.*;

/** Own GL-state boundary; never patches or replaces another renderer. Matrices belong to caller. */
final class RenderState implements AutoCloseable {
    private final int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    RenderState() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        // Immediate-mode vertices do not use client texture arrays. Some render bridges
        // deliberately reject glClientActiveTexture, so do not touch that state.
        GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
    public void close() {
        GL11.glPopClientAttrib();GL11.glPopAttrib();
        GL20.glUseProgram(program);GL13.glActiveTexture(active);
    }
}
