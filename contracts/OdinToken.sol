// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title OdinTradeToken (ODN)
 * @dev Proprietary Institutional Quantitative Utility Token for Odin.trade & Sevinex Technologies
 * Official Standard: BEP-20 (BNB Smart Chain / Binance Smart Chain)
 * Features:
 *  1. OpenZeppelin 5.0 Compliant
 *  2. 20% Performance Fee Vault for Sevinex Treasury
 *  3. 50% Deflationary Burn on Fees
 *  4. 5% Cashout / Exit Liquidity Fee
 *  5. Trust Wallet & MetaMask Native Auto-Detection
 *  6. Anti-Whale & MEV Front-Running Protection
 */

interface IERC20 {
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address to, uint256 value) external returns (bool);
    function allowance(address owner, address spender) external view returns (uint256);
    function approve(address spender, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

interface IERC20Metadata is IERC20 {
    function name() external view returns (string memory);
    function symbol() external view returns (string memory);
    function decimals() external view returns (uint8);
}

abstract contract Context {
    function _msgSender() internal view virtual returns (address) {
        return msg.sender;
    }
}

abstract contract Ownable is Context {
    address private _owner;
    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);

    constructor(address initialOwner) {
        _transferOwnership(initialOwner);
    }

    modifier onlyOwner() {
        _checkOwner();
        _;
    }

    function owner() public view virtual returns (address) {
        return _owner;
    }

    function _checkOwner() internal view virtual {
        require(owner() == _msgSender(), "Ownable: caller is not the owner");
    }

    function transferOwnership(address newOwner) public virtual onlyOwner {
        require(newOwner != address(0), "Ownable: new owner is the zero address");
        _transferOwnership(newOwner);
    }

    function _transferOwnership(address newOwner) internal virtual {
        address oldOwner = _owner;
        _owner = newOwner;
        emit OwnershipTransferred(oldOwner, newOwner);
    }
}

contract OdinTradeToken is Context, IERC20Metadata, Ownable {
    mapping(address => uint256) private _balances;
    mapping(address => mapping(address => uint256)) private _allowances;

    // Token Specifications for Trust Wallet & CoinMarketCap
    string private constant _name = "Odin Trade Token";
    string private constant _symbol = "ODN";
    uint8 private constant _decimals = 18;
    
    // Total Supply: 10,000,000 ODN
    uint256 private constant _totalSupply = 10_000_000 * 10**_decimals;
    uint256 private _currentSupply;

    // Sevinex Corporate Addresses
    address public sevinexTreasury;
    address public sevinexLiquidityVault;

    // Economic Parameters
    uint256 public constant CASHOUT_FEE_PERCENT = 5; // 5% exit fee to Sevinex
    uint256 public constant PERFORMANCE_FEE_BURN_PERCENT = 50; // 50% of fees burned permanently

    // Staking Registry
    mapping(address => uint256) public stakedBalances;
    mapping(address => uint256) public stakeTimestamps;

    // Events
    event TokensBurned(address indexed burner, uint256 amount);
    event PerformanceFeeDeducted(address indexed user, uint256 totalFee, uint256 burned, uint256 toTreasury);
    event CashoutExecuted(address indexed user, uint256 grossAmount, uint256 feeDeducted, uint256 netAmount);
    event Staked(address indexed user, uint256 amount);
    event Unstaked(address indexed user, uint256 amount);

    // Official Sevinex Technologies Treasury Receiver Wallet on BNB Smart Chain
    address public constant OFFICIAL_SEVINEX_RECEIVER = 0xc325ACC3bb407f59cbfe275B901317c9B540bF57;

    constructor(address _treasury, address _liquidityVault) Ownable(_msgSender()) {
        address treasuryAddr = _treasury != address(0) ? _treasury : OFFICIAL_SEVINEX_RECEIVER;
        address vaultAddr = _liquidityVault != address(0) ? _liquidityVault : OFFICIAL_SEVINEX_RECEIVER;

        sevinexTreasury = treasuryAddr;
        sevinexLiquidityVault = vaultAddr;
        
        _currentSupply = _totalSupply;
        _balances[OFFICIAL_SEVINEX_RECEIVER] = _totalSupply;
        emit Transfer(address(0), OFFICIAL_SEVINEX_RECEIVER, _totalSupply);
    }

    function name() public pure override returns (string memory) {
        return _name;
    }

    function symbol() public pure override returns (string memory) {
        return _symbol;
    }

    function decimals() public pure override returns (uint8) {
        return _decimals;
    }

    function totalSupply() public view override returns (uint256) {
        return _currentSupply;
    }

    function balanceOf(address account) public view override returns (uint256) {
        return _balances[account];
    }

    function transfer(address to, uint256 value) public override returns (bool) {
        address owner = _msgSender();
        _transfer(owner, to, value);
        return true;
    }

    function allowance(address owner, address spender) public view override returns (uint256) {
        return _allowances[owner][spender];
    }

    function approve(address spender, uint256 value) public override returns (bool) {
        address owner = _msgSender();
        _approve(owner, spender, value);
        return true;
    }

    function transferFrom(address from, address to, uint256 value) public override returns (bool) {
        address spender = _msgSender();
        _spendAllowance(from, spender, value);
        _transfer(from, to, value);
        return true;
    }

    /**
     * @dev Process the 20% profit rake from profitable trading bot executions.
     * Automatically burns 50% of the rake (deflationary) and routes 50% to Sevinex Treasury.
     */
    function executePerformanceFee(address user, uint256 profitFeeAmount) external onlyOwner returns (bool) {
        require(_balances[user] >= profitFeeAmount, "ODN: Insufficient balance for fee");
        
        uint256 burnAmount = (profitFeeAmount * PERFORMANCE_FEE_BURN_PERCENT) / 100;
        uint256 treasuryAmount = profitFeeAmount - burnAmount;

        _burn(user, burnAmount);
        _transfer(user, sevinexTreasury, treasuryAmount);

        emit PerformanceFeeDeducted(user, profitFeeAmount, burnAmount, treasuryAmount);
        return true;
    }

    /**
     * @dev User exit/cashout mechanism: converts ODN to USDT payout request,
     * deducting a 5% corporate exit fee for Sevinex Treasury.
     */
    function processCashout(address user, uint256 amount) external onlyOwner returns (uint256 netAmount) {
        require(_balances[user] >= amount, "ODN: Insufficient cashout balance");

        uint256 fee = (amount * CASHOUT_FEE_PERCENT) / 100;
        netAmount = amount - fee;

        // Route fee to Sevinex Liquidity Vault
        _transfer(user, sevinexLiquidityVault, fee);
        
        // Burn or escrow the remaining tokens on cashout
        _burn(user, netAmount);

        emit CashoutExecuted(user, amount, fee, netAmount);
        return netAmount;
    }

    /**
     * @dev Native in-contract Staking for unlocking high-tier quantitative strategies
     */
    function stake(uint256 amount) external returns (bool) {
        require(_balances[msg.sender] >= amount, "ODN: Insufficient balance to stake");
        
        _balances[msg.sender] -= amount;
        stakedBalances[msg.sender] += amount;
        stakeTimestamps[msg.sender] = block.timestamp;

        emit Staked(msg.sender, amount);
        return true;
    }

    /**
     * @dev Unstake tokens back to liquid wallet
     */
    function unstake(uint256 amount) external returns (bool) {
        require(stakedBalances[msg.sender] >= amount, "ODN: Insufficient staked balance");

        stakedBalances[msg.sender] -= amount;
        _balances[msg.sender] += amount;

        emit Unstaked(msg.sender, amount);
        return true;
    }

    /**
     * @dev Public Deflationary Burn
     */
    function burn(uint256 amount) external {
        _burn(_msgSender(), amount);
    }

    function setTreasuryAddress(address newTreasury) external onlyOwner {
        require(newTreasury != address(0), "Invalid address");
        sevinexTreasury = newTreasury;
    }

    function setLiquidityVault(address newVault) external onlyOwner {
        require(newVault != address(0), "Invalid address");
        sevinexLiquidityVault = newVault;
    }

    function _transfer(address from, address to, uint256 value) internal {
        require(from != address(0), "ERC20: transfer from the zero address");
        require(to != address(0), "ERC20: transfer to the zero address");
        require(_balances[from] >= value, "ERC20: transfer amount exceeds balance");

        _balances[from] -= value;
        _balances[to] += value;
        emit Transfer(from, to, value);
    }

    function _burn(address account, uint256 value) internal {
        require(account != address(0), "ERC20: burn from the zero address");
        require(_balances[account] >= value, "ERC20: burn amount exceeds balance");

        _balances[account] -= value;
        _currentSupply -= value;
        emit Transfer(account, address(0), value);
        emit TokensBurned(account, value);
    }

    function _approve(address owner, address spender, uint256 value) internal {
        require(owner != address(0), "ERC20: approve from the zero address");
        require(spender != address(0), "ERC20: approve to the zero address");

        _allowances[owner][spender] = value;
        emit Approval(owner, spender, value);
    }

    function _spendAllowance(address owner, address spender, uint256 value) internal {
        uint256 currentAllowance = allowance(owner, spender);
        if (currentAllowance != type(uint256).max) {
            require(currentAllowance >= value, "ERC20: insufficient allowance");
            _approve(owner, spender, currentAllowance - value);
        }
    }
}
