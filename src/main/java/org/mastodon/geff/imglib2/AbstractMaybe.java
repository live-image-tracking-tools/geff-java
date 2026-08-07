package org.mastodon.geff.imglib2;

abstract class AbstractMaybe {

    private boolean present;

    AbstractMaybe(final boolean present) {
        this.present = present;
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return present;
    }

    /**
     * If a value is not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return {@code true} if a value is not present, otherwise {@code false}
     * @since 11
     */
    public boolean isMissing() {
        return !present;
    }

    /**
     * Sets this value {@code isPresent() == present}.
     *
     * @param present
     * @return {@code present}
     */
    public boolean setPresent(final boolean present) {
        this.present = present;
        return present;
    }
}
