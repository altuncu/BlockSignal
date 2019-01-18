import zonefile from 'zone-file'

// This will validate that a given username's owner address is associated with
//   the given app address
function fetchProfileValidateAppAddress (username, appAddress, appOrigin) {
  return getProfileJWT(username)
    .then((response) => {
      const jwt = response.profileJWT
      const ownerAddress = response.owner
      // this verifies that the token is signed by the owner address
      const profile = blockstack.verifyProfileToken(jwt, ownerAddress)
            .payload.claim
      if (!profile.apps)
        throw new Error('No app entry in this profile! Validation fails.')
      if (!profile.apps[appOrigin])
        throw new Error(`No entry for ${appOrigin} in profile! Validation fails.`)
      const appUrl = profile.apps[appOrigin]
      // make sure that the address in the gaia url for the app matches
      //     the provided app address
      const matches = appUrl.match(/([13][a-km-zA-HJ-NP-Z0-9]{26,35})/)
      if (!matches)
        throw new Error('Failed to parse address out of app url!')

      const expectedAppAddress = matches[matches.length - 1]
      if (expectedAppAddress !== appAddress) {
        throw new Error(`Expected app address ${expectedAppAddress} does not match provided ${appAddress}`)
      }

      return true

    })
}

// Fetch the most recent profile object for a user.
//   Unfortunately, you need to do this manually, as lookupProfile() doesn't return the
//   full JSON web token, which you need to perform the verification.
function getProfileJWT(username) {
  return blockstack.config.network.getNameInfo(username)
    .then((responseJSON) => {
      if (responseJSON.zonefile && responseJSON.address) {
        const zoneFileJSON = zonefile.parseZoneFile(responseJSON.zonefile)
        const profileURL = blockstack.getTokenFileUrl(zoneFileJSON)
        return fetch(profileURL)
          .then((resp) => resp.json())
          .then((profileJWT) => ({ owner: responseJSON.address,
                                   profileJWT: profileJWT[0].token }))
      } else {
        throw new Error('Name information did not return zonefile.')
      }
    })
}